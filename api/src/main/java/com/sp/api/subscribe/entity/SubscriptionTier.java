package com.sp.api.subscribe.entity;

/**
 * 구독 등급. 유료 구독자는 채널이 따로 정한 오시마크를 단다.
 */
public enum SubscriptionTier {
    BASIC,
    PAID;

    /**
     * 저장된 등급과 유료 만료 시각으로 지금 실제 등급을 가린다.
     *
     * 만료 시각이 없는 유료는 기한이 없는 유료다(결제가 꺼져 있을 때의 자리표시 전환).
     * 만료 시각이 있고 이미 지났으면 일반으로 본다. 만료를 따로 돌려 정리하지 않아도
     * 읽는 곳마다 이 함수를 거치면 늘 같은 답이 나온다.
     */
    public static SubscriptionTier effective(
            SubscriptionTier stored,
            java.time.LocalDateTime paidUntil,
            java.time.LocalDateTime now
    ) {

        if (stored == PAID && (paidUntil == null || paidUntil.isAfter(now))) {
            return PAID;
        }

        return BASIC;
    }
}
