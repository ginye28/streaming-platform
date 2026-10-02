package com.sp.api.subscribe.repository;

import com.sp.api.subscribe.entity.SubscriptionTier;

/** 시청자 한 명이 한 채널에서 가진 구독 등급. 방송 목록에서 잠금 여부를 한꺼번에 가릴 때 쓴다. */
public record ChannelTier(Long channelId, SubscriptionTier tier, java.time.LocalDateTime paidUntil) {

    /** 지금 실제 등급. 유료 기간이 끝났으면 일반이다. */
    public SubscriptionTier effectiveTier(java.time.LocalDateTime now) {
        return SubscriptionTier.effective(tier, paidUntil, now);
    }
}
