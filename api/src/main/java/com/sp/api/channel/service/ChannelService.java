package com.sp.api.channel.service;

import com.sp.api.channel.dto.ChannelResponse;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.entity.SubscriptionTier;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelService {

    private final UserRepository userRepository;
    private final SubscribeRepository subscribeRepository;
    private final StreamRepository streamRepository;
    private final LiveStreamRepository liveStreamRepository;

    public ChannelResponse findChannel(Long channelId, String viewerEmail) {

        User channel = userRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("채널을 찾을 수 없습니다."));

        Subscribe mine = subscriptionOf(viewerEmail, channelId);

        return ChannelResponse.of(
                channel,
                subscribeRepository.countByChannelId(channelId),
                streamRepository.countByUserId(channelId),
                liveStreamRepository.existsByUserIdAndStatus(channelId, LiveStream.Status.LIVE),
                mine != null,
                mine == null ? null : mine.effectiveTier(LocalDateTime.now()),
                mine != null && mine.isMarkVisible(),
                paidUntilOf(mine)
        );
    }

    /** 내가 구독 중인 채널 목록. */
    public PageResponse<ChannelResponse> findMySubscriptions(String email, Pageable pageable) {

        User subscriber = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        return PageResponse.from(
                subscribeRepository.findBySubscriberId(subscriber.getId(), pageable)
                        .map(subscribe -> {
                            User channel = subscribe.getChannel();
                            return ChannelResponse.of(
                                    channel,
                                    subscribeRepository.countByChannelId(channel.getId()),
                                    streamRepository.countByUserId(channel.getId()),
                                    liveStreamRepository.existsByUserIdAndStatus(
                                            channel.getId(), LiveStream.Status.LIVE),
                                    true,
                                    subscribe.effectiveTier(LocalDateTime.now()),
                                    subscribe.isMarkVisible(),
                                    paidUntilOf(subscribe)
                            );
                        })
        );
    }

    /** 유료로 이용 중일 때만 만료 시각을 알려 준다. 이미 끝났거나 일반이면 null. */
    private LocalDateTime paidUntilOf(Subscribe subscribe) {

        if (subscribe == null
                || subscribe.effectiveTier(LocalDateTime.now()) != SubscriptionTier.PAID) {
            return null;
        }

        return subscribe.getPaidUntil();
    }

    /** 보는 사람의 이 채널 구독. 비로그인이거나 구독 중이 아니면 null. */
    private Subscribe subscriptionOf(String viewerEmail, Long channelId) {

        if (viewerEmail == null) {
            return null;
        }

        return userRepository.findByEmail(viewerEmail)
                .flatMap(viewer -> subscribeRepository
                        .findBySubscriberIdAndChannelId(viewer.getId(), channelId))
                .orElse(null);
    }
}
