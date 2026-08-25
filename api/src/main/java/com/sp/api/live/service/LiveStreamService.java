package com.sp.api.live.service;

import com.sp.api.block.service.BlockService;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.config.LiveProperties;
import com.sp.api.live.dto.LiveSettingRequest;
import com.sp.api.live.dto.LiveSettingResponse;
import com.sp.api.live.dto.LiveStreamResponse;
import com.sp.api.live.entity.LiveSetting;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveSettingRepository;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.notification.service.NotificationService;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    /**
     * OBS 가 송출을 시작할 때 nginx-rtmp 가 넘겨준 스트림 키를 검증하고 방송을 연다.
     * 이 검증이 없으면 누구나 임의의 이름으로 송출할 수 있다.
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

        user.assignPublicNameIfMissing();

        // 직전 방송이 종료 콜백을 못 받고 남아 있을 수 있다.
        liveStreamRepository.findByUserIdAndStatus(user.getId(), LiveStream.Status.LIVE)
                .ifPresent(stale -> {
                    log.warn("이전 방송이 종료되지 않아 정리합니다. liveId={}", stale.getId());
                    stale.end();
                });

        LiveSetting setting = liveSettingRepository.findByUserId(user.getId()).orElse(null);

        LiveStream live = new LiveStream(
                user,
                setting != null ? setting.getTitle() : user.getNickname() + " 님의 방송",
                setting != null ? setting.getDescription() : null,
                setting != null ? setting.getThumbnailUrl() : null,
                user.getPublicName()
        );

        liveStreamRepository.save(live);

        int notified = notificationService.notifyLiveStart(live);

        log.info("방송 시작: liveId={}, 알림 {}건", live.getId(), notified);

        return live;
    }

    /**
     * 공개 이름으로 다시 들어온 송출(리다이렉트 재진입)인지 확인한다.
     *
     * 공개 이름은 UUID 라 추측하기 어렵지만, 그것만 믿지 않고
     * "방금 스트림 키로 방송을 연 직후"라는 시간 창까지 함께 요구한다.
     */
    @Transactional(readOnly = true)
    public boolean isActiveRepublish(String publicName) {

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(REPUBLISH_WINDOW_SECONDS);

        return userRepository.findByPublicName(publicName)
                .flatMap(user -> liveStreamRepository
                        .findByUserIdAndStatus(user.getId(), LiveStream.Status.LIVE))
                .map(live -> live.getStartedAt().isAfter(threshold))
                .orElse(false);
    }

    /** 송출이 끊기면 방송을 닫는다. 공개 이름·스트림 키 어느 쪽으로 와도 처리한다. */
    @Transactional
    public void endBroadcast(String name) {

        if (name == null || name.isBlank()) {
            return;
        }

        userRepository.findByPublicName(name)
                .or(() -> userRepository.findByStreamKey(name))
                .flatMap(user -> liveStreamRepository
                        .findByUserIdAndStatus(user.getId(), LiveStream.Status.LIVE))
                .ifPresent(live -> {
                    live.end();
                    viewerTracker.clear(live.getId());
                    log.info("방송 종료: liveId={}", live.getId());
                });
    }

    /** 지금 방송 중인 목록. 로그인 상태면 내가 차단한 채널은 뺀다. */
    public PageResponse<LiveStreamResponse> findLiveNow(Pageable pageable, String viewerEmail) {
        return PageResponse.from(
                liveStreamRepository
                        .findByStatusExcludingUsers(
                                LiveStream.Status.LIVE, blockService.excludedUserIds(viewerEmail), pageable)
                        .map(this::toResponse)
        );
    }

    public LiveStreamResponse findById(Long id) {
        return toResponse(liveStreamRepository.findWithUserById(id)
                .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다.")));
    }

    /** 채널이 지금 방송 중이면 그 방송을, 아니면 404. */
    public LiveStreamResponse findLiveByChannel(Long channelId) {
        return toResponse(liveStreamRepository
                .findByUserIdAndStatus(channelId, LiveStream.Status.LIVE)
                .orElseThrow(() -> new NotFoundException("방송 중이 아닙니다.")));
    }

    /** 채널의 지난 방송 기록. */
    public PageResponse<LiveStreamResponse> findChannelHistory(Long channelId, Pageable pageable) {
        return PageResponse.from(
                liveStreamRepository
                        .findByUserIdAndStatusOrderByStartedAtDesc(
                                channelId, LiveStream.Status.ENDED, pageable)
                        .map(this::toResponse)
        );
    }

    public LiveSettingResponse getSetting(String email) {

        User user = findUser(email);

        return liveSettingRepository.findByUserId(user.getId())
                .map(LiveSettingResponse::from)
                .orElseGet(() -> new LiveSettingResponse(
                        user.getNickname() + " 님의 방송", null, null));
    }

    @Transactional
    public LiveSettingResponse updateSetting(String email, LiveSettingRequest request) {

        User user = findUser(email);

        LiveSetting setting = liveSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> liveSettingRepository.save(new LiveSetting(
                        user, request.getTitle(), request.getDescription(), request.getThumbnailUrl())));

        setting.update(request.getTitle(), request.getDescription(), request.getThumbnailUrl());

        return LiveSettingResponse.from(setting);
    }

    private LiveStreamResponse toResponse(LiveStream live) {
        return LiveStreamResponse.of(
                live,
                liveProperties.getHlsBaseUrl(),
                viewerTracker.count(live.getId())
        );
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
