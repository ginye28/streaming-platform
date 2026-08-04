package com.sp.api.subscribe.service;

import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscribeService {

    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;

    public boolean toggle(Long channelId, String email) {

        User subscriber = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        User channel = userRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("채널을 찾을 수 없습니다."));

        if (subscriber.getId().equals(channel.getId())) {
            throw new IllegalArgumentException("자기 자신은 구독할 수 없습니다.");
        }

        return subscribeRepository
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
    }

    public long count(Long channelId) {
        return subscribeRepository.countByChannelId(channelId);
    }
}
