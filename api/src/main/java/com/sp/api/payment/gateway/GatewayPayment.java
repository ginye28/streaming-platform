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
}
