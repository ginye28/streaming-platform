package com.sp.api.subscribe.dto;

import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.entity.SubscriptionTier;

import java.time.LocalDateTime;

/** 내가 이 채널에 설정해 둔 구독 상태. */
public record SubscriptionSettingResponse(
        boolean subscribed,
        SubscriptionTier tier,
        boolean markVisible,
        /** 유료 구독이 끝나는 때. 유료가 아니거나 기한이 없으면 null. */
        LocalDateTime paidUntil
) {

    public static SubscriptionSettingResponse from(Subscribe subscribe) {

        LocalDateTime now = LocalDateTime.now();
        SubscriptionTier tier = subscribe.effectiveTier(now);

        return new SubscriptionSettingResponse(
                true,
                tier,
                subscribe.isMarkVisible(),
                tier == SubscriptionTier.PAID ? subscribe.getPaidUntil() : null
        );
    }
}
