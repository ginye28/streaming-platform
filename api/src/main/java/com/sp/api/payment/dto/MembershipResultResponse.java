package com.sp.api.payment.dto;

import java.time.LocalDateTime;

/** 결제가 끝난 뒤 화면에 보여 줄 결과. */
public record MembershipResultResponse(
        Long channelId,
        String channelNickname,
        /** 지금 구독 등급. 결제가 끝났으니 PAID. */
        String tier,
        /** 유료 구독이 끝나는 때. */
        LocalDateTime paidUntil,
        int amount,
        String method,
        String receiptUrl
) {
}
