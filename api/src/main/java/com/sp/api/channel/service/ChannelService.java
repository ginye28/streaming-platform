package com.sp.api.channel.service;

import com.sp.api.channel.dto.ChannelResponse;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelService {

    private final UserRepository userRepository;
    private final SubscribeRepository subscribeRepository;
    private final StreamRepository streamRepository;

    public ChannelResponse findChannel(Long channelId, String viewerEmail) {

        User channel = userRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("채널을 찾을 수 없습니다."));

        return ChannelResponse.of(
                channel,
                subscribeRepository.countByChannelId(channelId),
                streamRepository.countByUserId(channelId),
                isSubscribedBy(viewerEmail, channelId)
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
                                    true
                            );
                        })
        );
    }

    private boolean isSubscribedBy(String viewerEmail, Long channelId) {

        if (viewerEmail == null) {
            return false;
        }

        return userRepository.findByEmail(viewerEmail)
                .map(viewer -> subscribeRepository
                        .existsBySubscriberIdAndChannelId(viewer.getId(), channelId))
                .orElse(false);
    }
}
