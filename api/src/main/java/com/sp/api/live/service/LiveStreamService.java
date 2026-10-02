package com.sp.api.live.service;

import com.sp.api.block.service.BlockService;
import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.moderation.ChatModerationService;
import com.sp.api.chat.moderation.ChatRole;
import com.sp.api.chat.moderation.ChatRoleService;
import com.sp.api.chat.moderation.SlowModeLimiter;
import com.sp.api.chat.repository.ChatMessageRepository;
import com.sp.api.chat.service.ChatMessageAssembler;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.intro.service.IntroService;
import com.sp.api.live.access.LiveAccessService;
import com.sp.api.live.config.LiveProperties;
import com.sp.api.live.dto.LiveSettingRequest;
import com.sp.api.live.dto.LiveSettingResponse;
import com.sp.api.live.dto.LiveStreamResponse;
import com.sp.api.live.entity.Audience;
import com.sp.api.live.entity.LiveSetting;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveSettingRepository;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.live.schedule.LiveSchedule;
import com.sp.api.live.schedule.LiveScheduleService;
import com.sp.api.notification.service.NotificationService;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveStreamService {

    /** 스트림 키로 방송을 연 뒤 리다이렉트가 되돌아오기까지 허용하는 시간(초). */
    private static final long REPUBLISH_WINDOW_SECONDS = 30;

    private final LiveStreamRepository liveStreamRepository;
    private final LiveSettingRepository liveSettingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LiveViewerTracker viewerTracker;
    private final LiveProperties liveProperties;
    private final BlockService blockService;
    private final IntroService introService;
    private final LiveAccessService accessService;
    private final LiveScheduleService scheduleService;
    private final SlowModeLimiter slowModeLimiter;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageAssembler chatMessageAssembler;
    private final ChatRoleService chatRoleService;

    /**
     * OBS 가 송출을 시작할 때 nginx-rtmp 가 넘겨준 스트림 키를 검증하고 방송을 연다.
     * 이 검증이 없으면 누구나 임의의 이름으로 송출할 수 있다.
     *
     * 방송의 제목·설명·공개 대상은 가까운 예약이 있으면 그 예약에서, 없으면 미리 저장해 둔 방송 설정에서 가져온다.
     * 재생 이름은 방송마다 새로 만든다. 그래서 멤버십 전용 방송의 주소는 볼 수 있는 사람에게만 내려가고
     * (주소를 모르면 볼 수 없다), 다시보기도 방송마다 따로 남는다.
     */
    @Transactional
    public LiveStream startBroadcast(String streamKey) {

        if (streamKey == null || streamKey.isBlank()) {
            throw new ForbiddenException("스트림 키가 필요합니다.");
        }

        User user = userRepository.findByStreamKey(streamKey)
                .orElseThrow(() -> {
                    log.warn("알 수 없는 스트림 키로 송출 시도");
                    return new ForbiddenException("유효하지 않은 스트림 키입니다.");
                });

        // 직전 방송이 종료 콜백을 못 받고 남아 있을 수 있다.
        liveStreamRepository.findByUserIdAndStatus(user.getId(), LiveStream.Status.LIVE)
                .ifPresent(stale -> {
                    log.warn("이전 방송이 종료되지 않아 정리합니다. liveId={}", stale.getId());
                    close(stale);
                });

        LiveSchedule schedule = scheduleService.findClaimable(user.getId()).orElse(null);
        LiveSetting setting = liveSettingRepository.findByUserId(user.getId()).orElse(null);

        LiveStream live;

        if (schedule != null) {
            live = new LiveStream(
                    user,
                    schedule.getTitle(),
                    schedule.getDescription(),
                    schedule.getThumbnailUrl(),
                    newStreamName(),
                    schedule.getAudience(),
                    schedule.getChatAudience(),
                    setting != null ? setting.getSlowModeSeconds() : 0
            );
        } else {
            live = new LiveStream(
                    user,
                    setting != null ? setting.getTitle() : user.getNickname() + " 님의 방송",
                    setting != null ? setting.getDescription() : null,
                    setting != null ? setting.getThumbnailUrl() : null,
                    newStreamName(),
                    setting != null ? setting.getAudience() : Audience.ALL,
                    setting != null ? setting.getChatAudience() : Audience.ALL,
                    setting != null ? setting.getSlowModeSeconds() : 0
            );
        }

        liveStreamRepository.save(live);

        if (schedule != null) {
            schedule.markStarted(live.getId());
        }

        int notified = notificationService.notifyLiveStart(live);

        log.info("방송 시작: liveId={}, 예약={}, 알림 {}건", live.getId(), schedule != null, notified);

        return live;
    }

    /**
     * 재생 이름으로 다시 들어온 송출(리다이렉트 재진입)인지 확인한다.
     *
     * 재생 이름은 UUID 라 추측하기 어렵지만, 그것만 믿지 않고
     * "방금 스트림 키로 방송을 연 직후"라는 시간 창까지 함께 요구한다.
     */
    @Transactional(readOnly = true)
    public boolean isActiveRepublish(String streamName) {

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(REPUBLISH_WINDOW_SECONDS);

        return liveStreamRepository.findByStreamNameAndStatus(streamName, LiveStream.Status.LIVE)
                .map(live -> live.getStartedAt().isAfter(threshold))
                .orElse(false);
    }

    /** 송출이 끊기면 방송을 닫는다. 재생 이름·스트림 키 어느 쪽으로 와도 처리한다. */
    @Transactional
    public void endBroadcast(String name) {

        if (name == null || name.isBlank()) {
            return;
        }

        liveStreamRepository.findByStreamNameAndStatus(name, LiveStream.Status.LIVE)
                .or(() -> userRepository.findByPublicName(name)
                        .or(() -> userRepository.findByStreamKey(name))
                        .flatMap(user -> liveStreamRepository
                                .findByUserIdAndStatus(user.getId(), LiveStream.Status.LIVE)))
                .ifPresent(live -> {
                    close(live);
                    log.info("방송 종료: liveId={}", live.getId());
                });
    }

    private void close(LiveStream live) {
        live.end(liveProperties.isVodEnabled());
        viewerTracker.clear(live.getId());
        slowModeLimiter.clear(live.getId());
    }

    /** 지금 방송 중인 목록. 로그인 상태면 내가 차단한 채널은 뺀다. */
    public PageResponse<LiveStreamResponse> findLiveNow(Pageable pageable, String viewerEmail) {

        var page = liveStreamRepository.findByStatusExcludingUsers(
                LiveStream.Status.LIVE, blockService.excludedUserIds(viewerEmail), pageable);

        return PageResponse.of(page, toResponses(page.getContent(), viewerEmail));
    }

    /**
     * "이어보기" — 지금 방송 중인 것 중 이 시청자가 아직 안 본 방송 하나.
     *
     * 인트로에서 [다음 방송] 을 눌렀을 때 어디로 보낼지 정한다.
     * 이미 인트로를 본 채널과 차단한 채널은 뺀다. 넘긴 사람이 계속 다시 나오면
     * 넘긴 의미가 없기 때문이다.
     */
    public LiveStreamResponse findNext(String viewerEmail, String viewerKey, Long excludeLiveId) {

        Set<Long> excluded = new HashSet<>(blockService.excludedUserIds(viewerEmail));

        excluded.addAll(introService.seenChannelIds(viewerKey));

        if (excludeLiveId != null) {
            liveStreamRepository.findWithUserById(excludeLiveId)
                    .ifPresent(live -> excluded.add(live.getUser().getId()));
        }

        return liveStreamRepository
                .findByStatusExcludingUsers(LiveStream.Status.LIVE, excluded, Pageable.ofSize(1))
                .stream()
                .findFirst()
                .map(live -> toResponses(List.of(live), viewerEmail).get(0))
                .orElseThrow(() -> new NotFoundException("더 볼 방송이 없습니다."));
    }

    /** 방송 하나. 고정된 채팅까지 함께 내려 주고, 영상을 볼 수 없는 시청자에게는 재생 주소를 숨긴다. */
    public LiveStreamResponse findById(Long id, String viewerEmail) {

        LiveStream live = liveStreamRepository.findWithUserById(id)
                .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다."));

        return toDetailResponse(live, viewerEmail);
    }

    /** 채널이 지금 방송 중이면 그 방송을, 아니면 404. */
    public LiveStreamResponse findLiveByChannel(Long channelId, String viewerEmail) {

        LiveStream live = liveStreamRepository
                .findByUserIdAndStatus(channelId, LiveStream.Status.LIVE)
                .orElseThrow(() -> new NotFoundException("방송 중이 아닙니다."));

        return toResponses(List.of(live), viewerEmail).get(0);
    }

    /** 채널의 지난 방송 기록. */
    public PageResponse<LiveStreamResponse> findChannelHistory(Long channelId, Pageable pageable, String viewerEmail) {

        var page = liveStreamRepository.findByUserIdAndStatusOrderByStartedAtDesc(
                channelId, LiveStream.Status.ENDED, pageable);

        return PageResponse.of(page, toResponses(page.getContent(), viewerEmail));
    }

    public LiveSettingResponse getSetting(String email) {

        User user = findUser(email);

        return liveSettingRepository.findByUserId(user.getId())
                .map(LiveSettingResponse::from)
                .orElseGet(() -> new LiveSettingResponse(
                        user.getNickname() + " 님의 방송", null, null,
                        Audience.ALL.name(), Audience.ALL.name(), 0));
    }

    @Transactional
    public LiveSettingResponse updateSetting(String email, LiveSettingRequest request) {

        User user = findUser(email);

        int slowMode = request.getSlowModeSeconds() == null ? 0 : request.getSlowModeSeconds();

        if (!ChatModerationService.SLOW_MODE_CHOICES.contains(slowMode)) {
            throw new BadRequestException("슬로우 모드는 " + ChatModerationService.SLOW_MODE_CHOICES.stream().sorted().toList()
                    + "초 중에서 고를 수 있습니다.");
        }

        LiveSetting setting = liveSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> liveSettingRepository.save(new LiveSetting(
                        user, request.getTitle(), request.getDescription(), request.getThumbnailUrl())));

        setting.update(
                request.getTitle(), request.getDescription(), request.getThumbnailUrl(),
                request.getAudience(), request.getChatAudience(), slowMode);

        return LiveSettingResponse.from(setting);
    }

    // ---- 응답 만들기 ----

    /** 목록용. 영상을 볼 수 있는지는 구독 등급을 한 번에 읽어서 가린다. */
    private List<LiveStreamResponse> toResponses(List<LiveStream> lives, String viewerEmail) {

        Set<Long> watchable = accessService.watchableLiveIds(lives, viewerEmail);

        return lives.stream()
                .map(live -> LiveStreamResponse.of(
                        live, liveProperties, viewerTracker.count(live.getId()), watchable.contains(live.getId()),
                        null, null, false))
                .toList();
    }

    private LiveStreamResponse toDetailResponse(LiveStream live, String viewerEmail) {

        boolean canWatch = accessService.canWatch(live, viewerEmail);

        ChatMessageResponse pinned = null;

        // 볼 수 없는 방송의 채팅은 보여 주지 않는다. 고정 메시지도 마찬가지다.
        if (canWatch && live.getPinnedMessageId() != null) {
            pinned = chatMessageRepository.findWithUserById(live.getPinnedMessageId())
                    .filter(message -> !message.isDeleted())
                    .map(message -> chatMessageAssembler.assembleOne(live, message))
                    .orElse(null);
        }

        User viewer = viewerEmail == null ? null : userRepository.findByEmail(viewerEmail).orElse(null);

        ChatRole role = viewer == null ? null : chatRoleService.roleOf(live.getUser().getId(), viewer.getId());

        boolean chatLocked = role == null
                && !accessService.canUse(live.getChatAudience(), live.getUser().getId(), viewerEmail);

        return LiveStreamResponse.of(
                live, liveProperties, viewerTracker.count(live.getId()), canWatch, pinned, role, chatLocked);
    }

    private static String newStreamName() {
        return UUID.randomUUID().toString();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
