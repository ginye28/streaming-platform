package com.sp.api.live.entity;

import com.sp.api.subscribe.entity.SubscriptionTier;

/**
 * 방송이나 채팅을 누가 쓸 수 있는지.
 *
 * 방송 영상에는 "보는 사람" 으로, 채팅에는 "쓰는 사람" 으로 쓴다. 두 값은 서로 따로 정한다
 * (예: 누구나 보되 채팅은 구독자만).
 */
public enum Audience {

    /** 누구나. 로그인하지 않아도 된다. */
    ALL,

    /** 이 채널을 구독한 사람(일반·유료 모두). */
    SUBSCRIBERS,

    /** 지금 유료로 이용 중인 구독자만. 유료 기간이 끝났으면 일반 구독자와 같다. */
    PAID;

    /**
     * @param tier 이 사람의 이 채널 구독 등급. 구독하지 않았으면 null.
     */
    public boolean allows(SubscriptionTier tier) {
        return switch (this) {
            case ALL -> true;
            case SUBSCRIBERS -> tier != null;
            case PAID -> tier == SubscriptionTier.PAID;
        };
    }

    public boolean isRestricted() {
        return this != ALL;
    }

    /** 요청에 값이 없으면 제한 없음으로 본다. */
    public static Audience orAll(Audience value) {
        return value == null ? ALL : value;
    }
}
