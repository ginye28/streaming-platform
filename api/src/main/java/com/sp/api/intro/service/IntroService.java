package com.sp.api.intro.service;

import com.sp.api.common.exception.NotFoundException;
import com.sp.api.intro.dto.IntroRequest;
import com.sp.api.intro.dto.IntroResponse;
import com.sp.api.intro.entity.ChannelIntro;
import com.sp.api.intro.entity.IntroImpression;
import com.sp.api.intro.repository.ChannelIntroRepository;
import com.sp.api.intro.repository.IntroImpressionRepository;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 처음 들어온 시청자에게 "이 사람이 누구인지" 를 먼저 보여 준다.
 *
 * 낯선 방송에 들어가면 잡담 한복판에 떨어져 그대로 나가 버리는 것을 막기 위한 기능이다.
 * 다만 매번 뜨면 그건 광고다. 아래 shouldShowGate 의 조건이 그 선을 지킨다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IntroService {

    private final ChannelIntroRepository introRepository;
    private final IntroImpressionRepository impressionRepository;
    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;
    private final LiveStreamRepository liveStreamRepository;

    /**
     * 채널의 인트로. 이 시청자에게 띄울지(showGate)까지 서버가 정해서 준다.
     *
     * @param viewerKey 로그인했으면 계정, 아니면 접속 IP
     */
    public IntroResponse findForViewer(Long channelId, String viewerKey, String viewerEmail) {

        User channel = userRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("채널을 찾을 수 없습니다."));

        ChannelIntro intro = introRepository.findByUserId(channelId).orElse(null);
        boolean subscribed = isSubscribedBy(viewerEmail, channelId);

        return new IntroResponse(
                channel.getId(),
                channel.getNickname(),
                channel.getProfileImage(),
                intro == null ? null : intro.getVideoUrl(),
                intro == null ? null : intro.getHeadline(),
                intro == null ? null : intro.getGreeting(),
                subscribeRepository.countByChannelId(channelId),
                subscribed,
                shouldShowGate(intro, channelId, viewerKey, viewerEmail, subscribed)
        );
    }

    /**
     * 인트로를 띄울 조건. 하나라도 어긋나면 바로 방송으로 들여보낸다.
     *
     * - 올려 둔 인트로가 있을 것
     * - 이미 본 채널이 아닐 것
     * - 구독 중인 채널이 아닐 것 (이미 아는 사람이다)
     * - 자기 방송이 아닐 것
     */
    private boolean shouldShowGate(
            ChannelIntro intro, Long channelId, String viewerKey, String viewerEmail, boolean subscribed) {

        if (intro == null || intro.isEmpty() || subscribed) {
            return false;
        }

        if (isOwnChannel(viewerEmail, channelId)) {
            return false;
        }

        return impressionRepository.findByViewerKeyAndChannelId(viewerKey, channelId).isEmpty();
    }

    /**
     * 방송 id 로 그 채널의 인트로를 찾는다.
     *
     * 화면이 방송 정보와 인트로를 나란히 받을 수 있어야 해서 둔 통로다.
     * 채널 id 를 알아내려고 방송을 먼저 받고 나면, 그사이 방송이 잠깐 보였다 가려진다.
     */
    public IntroResponse findForLive(Long liveId, String viewerKey, String viewerEmail) {

        Long channelId = liveStreamRepository.findWithUserById(liveId)
                .map(live -> live.getUser().getId())
                .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다."));

        return findForViewer(channelId, viewerKey, viewerEmail);
    }

    /** 인트로를 보고 무엇을 했는지 남긴다. 같은 채널을 다시 보면 마지막 행동만 남는다. */
    @Transactional
    public void recordSeen(Long channelId, String viewerKey, IntroImpression.Action action) {

        User channel = userRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("채널을 찾을 수 없습니다."));

        impressionRepository.findByViewerKeyAndChannelId(viewerKey, channelId)
                .ifPresentOrElse(
                        impression -> impression.record(action),
                        () -> impressionRepository.save(new IntroImpression(viewerKey, channel, action))
                );
    }

    /** "이어보기" 에서 건너뛸 채널들 — 이미 인트로를 본 곳이다. */
    public List<Long> seenChannelIds(String viewerKey) {
        return impressionRepository.findSeenChannelIds(viewerKey);
    }

    public IntroResponse findMine(String email) {

        User me = findUser(email);

        return findForViewer(me.getId(), "user:" + email, email);
    }

    @Transactional
    public IntroResponse updateMine(String email, IntroRequest request) {

        User me = findUser(email);

        introRepository.findByUserId(me.getId())
                .ifPresentOrElse(
                        intro -> intro.update(
                                request.getVideoUrl(), request.getHeadline(), request.getGreeting()),
                        () -> introRepository.save(new ChannelIntro(
                                me, request.getVideoUrl(), request.getHeadline(), request.getGreeting()))
                );

        return findMine(email);
    }

    private boolean isOwnChannel(String viewerEmail, Long channelId) {

        if (viewerEmail == null) {
            return false;
        }

        return userRepository.findByEmail(viewerEmail)
                .map(viewer -> viewer.getId().equals(channelId))
                .orElse(false);
    }

    private boolean isSubscribedBy(String viewerEmail, Long channelId) {

        if (viewerEmail == null) {
            return false;
        }

        return userRepository.findByEmail(viewerEmail)
                .map(viewer -> subscribeRepository.existsBySubscriberIdAndChannelId(viewer.getId(), channelId))
                .orElse(false);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
