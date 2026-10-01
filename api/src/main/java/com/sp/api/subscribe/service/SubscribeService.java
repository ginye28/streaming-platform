package com.sp.api.subscribe.service;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.payment.config.PaymentProperties;
import com.sp.api.subscribe.dto.SubscribeResponse;
import com.sp.api.subscribe.dto.SubscriptionSettingResponse;
import com.sp.api.subscribe.dto.UpdateSubscriptionRequest;
import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.entity.SubscriptionTier;
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
    private final PaymentProperties paymentProperties;

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
     * 유료(PAID) 전환은 결제가 켜져 있으면(토스 키가 있으면) 이 API 로 할 수 없고 결제로만 된다.
     * 결제가 꺼져 있을 때만 결제 없이 바뀌는 자리표시로 남는다(개발·데모용).
     * 일반(BASIC)으로 내리는 것은 언제든 되며, 남은 유료 기간은 없어진다.
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

        if (request.getTier() == SubscriptionTier.PAID && paymentProperties.isEnabled()) {
            throw new BadRequestException("유료 구독은 결제로만 시작할 수 있습니다.");
        }

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
