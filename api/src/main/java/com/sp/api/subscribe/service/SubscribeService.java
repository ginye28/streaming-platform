package com.sp.api.subscribe.service;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.subscribe.dto.SubscribeResponse;
import com.sp.api.subscribe.dto.SubscriptionSettingResponse;
import com.sp.api.subscribe.dto.UpdateSubscriptionRequest;
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
                    subscribeRepository.save(new Subscribe(subscriber, channel));
                    return true;
                });

        subscribeRepository.flush();

        return new SubscribeResponse(
                subscribed,
                subscribeRepository.countByChannelId(channel.getId())
        );
    }

    /**
     * 내 구독 설정(등급, 마크 표시 여부)을 바꾼다. 구독 중인 채널만 바꿀 수 있다.
     *
     * 유료(PAID) 전환은 아직 결제와 이어져 있지 않다. 결제를 붙이면 이 자리를
     * 결제 완료 처리로 바꾸고, 사용자가 직접 PAID 를 요청하지 못하게 막으면 된다.
     * 구독을 해제하면 행이 지워지므로 등급과 표시 설정도 함께 초기화된다.
     */
    @Transactional
    public SubscriptionSettingResponse update(
            Long channelId, String email, UpdateSubscriptionRequest request
    ) {

        if (request.getTier() == null && request.getMarkVisible() == null) {
            throw new BadRequestException("바꿀 값(tier 또는 markVisible)을 보내 주세요.");
        }

        User subscriber = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Subscribe subscribe = subscribeRepository
                .findBySubscriberIdAndChannelId(subscriber.getId(), channelId)
                .orElseThrow(() -> new BadRequestException("구독 중인 채널만 설정할 수 있습니다."));

        if (request.getTier() != null) {
            subscribe.changeTier(request.getTier());
        }

        if (request.getMarkVisible() != null) {
            subscribe.showMark(request.getMarkVisible());
        }

        return SubscriptionSettingResponse.from(subscribe);
    }

    public long count(Long channelId) {
        return subscribeRepository.countByChannelId(channelId);
    }
}
