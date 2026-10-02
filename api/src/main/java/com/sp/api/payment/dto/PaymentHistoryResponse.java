package com.sp.api.payment.dto;

import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

/** 내 결제 내역 한 줄. 승인된 결제와 환불된 결제가 나온다. */
public record PaymentHistoryResponse(
        Long id,
        Long channelId,
        String channelNickname,
        int amount,
        String method,
        /** DONE(승인) 또는 CANCELED(환불). */
        String status,
        LocalDateTime approvedAt,
        LocalDateTime canceledAt,
        String receiptUrl,
        /** 지금 직접 환불할 수 있는지. 승인된 결제이고 환불 가능 기간 안일 때만 true. */
        boolean refundable,
        /** 직접 환불할 수 있는 마지막 때. 승인된 결제에만 있다. */
        LocalDateTime refundDeadline,
        /** SUBSCRIPTION(유료 구독) 또는 DONATION(후원). */
        String kind
) {

    public static PaymentHistoryResponse from(Payment payment, int refundWindowDays, LocalDateTime now) {

        boolean done = payment.getStatus() == PaymentStatus.DONE;

        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getChannel().getId(),
                payment.getChannel().getNickname(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus().name(),
                payment.getApprovedAt(),
                payment.getCanceledAt(),
                payment.getReceiptUrl(),
                // 후원은 사용자가 직접 환불하지 못한다.
                done && !payment.isDonation() && payment.isWithinRefundWindow(refundWindowDays, now),
                done && !payment.isDonation() && payment.getApprovedAt() != null
                        ? payment.getApprovedAt().plusDays(refundWindowDays)
                        : null,
                payment.getKind().name()
        );
    }
}
