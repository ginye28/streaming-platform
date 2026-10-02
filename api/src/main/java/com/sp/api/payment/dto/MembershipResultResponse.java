package com.sp.api.payment.dto;

import java.time.LocalDateTime;

/** 결제가 끝난 뒤 화면에 보여 줄 결과. 구독과 후원이 같은 모양을 쓴다. */
public record MembershipResultResponse(
        Long channelId,
        String channelNickname,
        /** 구독 결제면 지금 구독 등급(PAID). 후원이면 null. */
        String tier,
        /** 구독 결제면 유료 구독이 끝나는 때. 후원이면 null. */
        LocalDateTime paidUntil,
        int amount,
        String method,
        String receiptUrl,
        /** SUBSCRIPTION 또는 DONATION. */
        String kind,
        /** 후원이면 후원한 방송. 구독이면 null. */
        Long liveId
) {
}
