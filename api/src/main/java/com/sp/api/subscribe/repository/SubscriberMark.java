package com.sp.api.subscribe.repository;

import com.sp.api.subscribe.entity.SubscriptionTier;

/** 구독자 한 명의 등급과 마크 표시 여부. 오시마크를 가릴 때 쓰는 최소한의 값. */
public record SubscriberMark(
        Long subscriberId,
        SubscriptionTier tier,
        boolean markVisible,
        java.time.LocalDateTime paidUntil
) {

    /** 지금 실제 등급. 유료 기간이 끝났으면 일반이다. */
    public SubscriptionTier effectiveTier(java.time.LocalDateTime now) {
        return SubscriptionTier.effective(tier, paidUntil, now);
    }
}
