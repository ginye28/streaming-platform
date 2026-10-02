package com.sp.api.live.schedule;

import com.sp.api.block.service.BlockService;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.access.LiveAccessService;
import com.sp.api.notification.service.NotificationService;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * 방송 예약.
 *
 * - 만들면 구독자에게 알리고, 시각이 가까워지면 한 번 더 알린다("곧 시작").
 * - 방송이 시작되면(송출이 들어오면) 가까운 예약 하나가 그 방송으로 이어지고, 예약에 적은 제목·설명·공개 대상이 쓰인다.
 * - 시각이 한참 지나도 방송하지 않은 예약은 목록에서 내린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveScheduleService {

    /** 한 채널이 동시에 걸어 둘 수 있는 예약 수. */
    static final int MAX_ACTIVE_PER_CHANNEL = 20;

    /** 예약은 이만큼 앞까지만 걸 수 있다. */
    static final int MAX_DAYS_AHEAD = 90;

    /** 방송 시각이 지나도 이 시간까지는 "곧 시작" 대기실을 보여 준다(방송이 조금 늦어지는 경우). */
    static final int GRACE_HOURS = 2;

    /** 시작 이 시간 전부터 "곧 시작" 알림을 보낸다. */
    static final int REMINDER_MINUTES = 10;

    /** 송출이 들어올 때 이 범위 안의 예약을 그 방송의 예약으로 본다. */
    private static final int CLAIM_BEFORE_MINUTES = 90;
    private static final int CLAIM_AFTER_HOURS = 6;

    /** 시각이 이보다 오래 지난 예약은 내린다. */
    private static final int EXPIRE_AFTER_HOURS = 6;

    private final LiveScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final BlockService blockService;
    private final LiveAccessService accessService;
    private final NotificationService notificationService;

    @Transactional
    public LiveScheduleResponse create(String email, LiveScheduleRequest request) {

        User user = findUser(email);

        LocalDateTime scheduledAt = toServerTime(request.scheduledAt());

        validateTime(scheduledAt);

        if (scheduleRepository.countByUserIdAndStatus(user.getId(), LiveSchedule.Status.SCHEDULED)
                >= MAX_ACTIVE_PER_CHANNEL) {
            throw new BadRequestException("예약은 최대 " + MAX_ACTIVE_PER_CHANNEL + "개까지 걸어 둘 수 있습니다.");
        }

        LiveSchedule schedule = scheduleRepository.save(new LiveSchedule(
                user,
                request.title(),
                request.description(),
                request.thumbnailUrl(),
                scheduledAt,
                request.audience(),
                request.chatAudience()
        ));

        int notified = notificationService.notifyScheduleCreated(schedule);

        log.info("방송 예약: scheduleId={}, 알림 {}건", schedule.getId(), notified);

        return LiveScheduleResponse.of(schedule, true);
    }

    @Transactional
    public LiveScheduleResponse update(String email, Long id, LiveScheduleRequest request) {

        LiveSchedule schedule = findOwned(email, id);

        LocalDateTime scheduledAt = toServerTime(request.scheduledAt());

        validateTime(scheduledAt);

        boolean timeChanged = schedule.update(
                request.title(),
                request.description(),
                request.thumbnailUrl(),
                scheduledAt,
                request.audience(),
                request.chatAudience()
        );

        // 시간이 바뀌었을 때만 알린다. 제목이나 설명을 고칠 때마다 알리면 구독자가 귀찮아진다.
        if (timeChanged) {
            notificationService.notifyScheduleChanged(schedule);
        }

        return LiveScheduleResponse.of(schedule, true);
    }

    @Transactional
    public void cancel(String email, Long id) {

        LiveSchedule schedule = findOwned(email, id);

        schedule.cancel();

        notificationService.notifyScheduleCanceled(schedule);
    }

    /** 앞으로의 방송 예약. 로그인했으면 내가 차단한 채널은 뺀다. */
    public PageResponse<LiveScheduleResponse> findUpcoming(Pageable pageable, String viewerEmail) {

        var page = scheduleRepository.findUpcoming(
                LocalDateTime.now().minusHours(GRACE_HOURS), blockService.excludedUserIds(viewerEmail), pageable);

        return PageResponse.of(page, page.getContent().stream()
                .map(schedule -> toResponse(schedule, viewerEmail))
                .toList());
    }

    public List<LiveScheduleResponse> findUpcomingOfChannel(Long channelId, String viewerEmail) {
        return scheduleRepository.findUpcomingOfChannel(channelId, LocalDateTime.now().minusHours(GRACE_HOURS)).stream()
                .map(schedule -> toResponse(schedule, viewerEmail))
                .toList();
    }

    /** 내 예약(방송 전인 것). 시각이 지났지만 아직 정리되지 않은 것도 보여서 취소할 수 있게 한다. */
    public List<LiveScheduleResponse> findMine(String email) {

        User user = findUser(email);

        return scheduleRepository
                .findByUserIdAndStatusOrderByScheduledAtAsc(user.getId(), LiveSchedule.Status.SCHEDULED).stream()
                .map(schedule -> LiveScheduleResponse.of(schedule, true))
                .toList();
    }

    /** 대기실이 읽는 예약 하나. 취소·만료된 것도 읽을 수 있어야 대기실이 "취소됐어요" 를 보여 줄 수 있다. */
    public LiveScheduleResponse findById(Long id, String viewerEmail) {

        LiveSchedule schedule = scheduleRepository.findWithUserById(id)
                .orElseThrow(() -> new NotFoundException("방송 예약을 찾을 수 없습니다."));

        return toResponse(schedule, viewerEmail);
    }

    /**
     * 방송이 시작될 때 이어 붙일 예약. 방송 시각이 가장 가까운 것 하나.
     * 이어 붙이는 것(markStarted)은 방송을 저장한 뒤 호출하는 쪽이 한다.
     */
    public Optional<LiveSchedule> findClaimable(Long userId) {

        LocalDateTime now = LocalDateTime.now();

        return scheduleRepository.findClaimable(
                        userId,
                        now.minusHours(CLAIM_AFTER_HOURS),
                        now.plusMinutes(CLAIM_BEFORE_MINUTES),
                        PageRequest.of(0, 1))
                .stream()
                .findFirst();
    }

    /** "곧 시작" 알림을 보낸다. 같은 예약에는 한 번만 보낸다. @return 보낸 예약 수 */
    @Transactional
    public int sendReminders() {

        LocalDateTime now = LocalDateTime.now();

        List<LiveSchedule> due = scheduleRepository.findDueForReminder(
                now.minusMinutes(30), now.plusMinutes(REMINDER_MINUTES));

        for (LiveSchedule schedule : due) {
            notificationService.notifyScheduleReminder(schedule);
            schedule.markReminderSent();
        }

        return due.size();
    }

    /** 시각이 한참 지난 채 방송하지 않은 예약을 내린다. @return 내린 예약 수 */
    @Transactional
    public int expireStale() {

        List<LiveSchedule> stale = scheduleRepository.findStale(LocalDateTime.now().minusHours(EXPIRE_AFTER_HOURS));

        stale.forEach(LiveSchedule::expire);

        return stale.size();
    }

    // ---- 도우미 ----

    private LiveScheduleResponse toResponse(LiveSchedule schedule, String viewerEmail) {
        return LiveScheduleResponse.of(
                schedule, accessService.canUse(schedule.getAudience(), schedule.getUser().getId(), viewerEmail));
    }

    /** 요청의 시각(시간대 포함)을 서버가 쓰는 시각(LocalDateTime)으로 바꾼다. 같은 순간을 가리키게 한다. */
    private static LocalDateTime toServerTime(OffsetDateTime value) {
        return value.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void validateTime(LocalDateTime scheduledAt) {

        LocalDateTime now = LocalDateTime.now();

        if (!scheduledAt.isAfter(now)) {
            throw new BadRequestException("방송 시각은 지금보다 뒤여야 합니다.");
        }

        if (scheduledAt.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new BadRequestException("방송 예약은 " + MAX_DAYS_AHEAD + "일 안으로만 걸 수 있습니다.");
        }
    }

    private LiveSchedule findOwned(String email, Long id) {

        User user = findUser(email);

        LiveSchedule schedule = scheduleRepository.findWithUserById(id)
                .orElseThrow(() -> new NotFoundException("방송 예약을 찾을 수 없습니다."));

        if (!schedule.isOwnedBy(user.getId())) {
            throw new ForbiddenException("내 예약이 아닙니다.");
        }

        if (!schedule.isScheduled()) {
            throw new BadRequestException("이미 시작됐거나 끝난 예약은 바꿀 수 없습니다.");
        }

        return schedule;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
