package com.sp.api.subscribe.dto;

import com.sp.api.subscribe.entity.SubscriptionTier;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 내 구독 설정 변경. 보낸 값만 바꾸고, 보내지 않은 값은 그대로 둔다.
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateSubscriptionRequest {

    /** BASIC 또는 PAID. */
    private SubscriptionTier tier;

    /** 이 채널의 오시마크를 내 이름 옆에 보일지. */
    private Boolean markVisible;
}
