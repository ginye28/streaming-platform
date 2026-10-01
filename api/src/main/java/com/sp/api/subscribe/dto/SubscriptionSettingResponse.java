package com.sp.api.subscribe.dto;

import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.entity.SubscriptionTier;

/** 내가 이 채널에 설정해 둔 구독 상태. */
public record SubscriptionSettingResponse(
        boolean subscribed,
        SubscriptionTier tier,
        boolean markVisible
) {

    public static SubscriptionSettingResponse from(Subscribe subscribe) {
        return new SubscriptionSettingResponse(true, subscribe.getTier(), subscribe.isMarkVisible());
    }
}
