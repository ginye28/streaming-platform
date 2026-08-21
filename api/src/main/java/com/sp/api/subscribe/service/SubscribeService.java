package com.sp.api.subscribe.service;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.subscribe.dto.SubscribeResponse;
import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscribeService {

    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;

    @Transactional
    public SubscribeResponse toggle(Long channelId, String email) {

        User subscriber = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        User channel = userRepository.findById(channelId)
                .orElseThrow(() -> new NotFoundException("채널을 찾을 수 없습니다."));

        if (subscriber.getId().equals(channel.getId())) {
            throw new BadRequestException("자기 자신은 구독할 수 없습니다.");
        }

        boolean subscribed = subscribeRepository
                .findBySubscriberIdAndChannelId(subscriber.getId(), channel.getId())
                .map(subscribe -> {
                    subscribeRepository.delete(subscribe);
                    return false;
                })
                .orElseGet(() -> {
                    Subscribe subscribe = new Subscribe();
                    subscribe.setSubscriber(subscriber);
                    subscribe.setChannel(channel);
                    subscribeRepository.save(subscribe);
                    return true;
                });

        subscribeRepository.flush();

        return new SubscribeResponse(
                subscribed,
                subscribeRepository.countByChannelId(channel.getId())
        );
    }

    public long count(Long channelId) {
        return subscribeRepository.countByChannelId(channelId);
    }
}
