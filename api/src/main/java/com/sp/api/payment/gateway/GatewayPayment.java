package com.sp.api.payment.gateway;

import java.time.LocalDateTime;

/** 결제사가 알려 준 결제 결과 중 우리가 쓰는 값만. */
public record GatewayPayment(
        String paymentKey,
        String orderId,
        /** DONE 이면 승인 완료. */
        String status,
        long totalAmount,
        String method,
        String receiptUrl,
        LocalDateTime approvedAt
) {

    public boolean isDone() {
        return "DONE".equals(status);
    }

    /** 전액 취소된 결제. 부분 취소(PARTIAL_CANCELED)는 취소로 보지 않는다. */
    public boolean isCanceled() {
        return "CANCELED".equals(status);
    }
}
