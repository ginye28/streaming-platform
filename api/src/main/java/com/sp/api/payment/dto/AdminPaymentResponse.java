package com.sp.api.payment.dto;

import com.sp.api.payment.entity.Payment;

import java.time.LocalDateTime;

/** 관리자가 보는 결제 한 줄. 결제한 사람과 채널, 실패·취소 사유까지 담는다. */
public record AdminPaymentResponse(
        Long id,
        String orderId,
        Long userId,
        String userEmail,
        String userNickname,
        Long channelId,
        String channelNickname,
        int amount,
        String status,
        String method,
        LocalDateTime createdAt,
        LocalDateTime approvedAt,
        LocalDateTime canceledAt,
        String cancelReason,
        String failureCode,
        String failureMessage
) {

    public static AdminPaymentResponse from(Payment payment) {
        return new AdminPaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUser().getId(),
                payment.getUser().getEmail(),
                payment.getUser().getNickname(),
                payment.getChannel().getId(),
                payment.getChannel().getNickname(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getMethod(),
                payment.getCreatedAt(),
                payment.getApprovedAt(),
                payment.getCanceledAt(),
                payment.getCancelReason(),
                payment.getFailureCode(),
                payment.getFailureMessage()
        );
    }
}
