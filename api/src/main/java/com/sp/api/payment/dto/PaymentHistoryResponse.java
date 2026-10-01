package com.sp.api.payment.dto;

import com.sp.api.payment.entity.Payment;

import java.time.LocalDateTime;

/** 내 결제 내역 한 줄. */
public record PaymentHistoryResponse(
        Long id,
        Long channelId,
        String channelNickname,
        int amount,
        String method,
        LocalDateTime approvedAt,
        String receiptUrl
) {

    public static PaymentHistoryResponse from(Payment payment) {
        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getChannel().getId(),
                payment.getChannel().getNickname(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getApprovedAt(),
                payment.getReceiptUrl()
        );
    }
}
