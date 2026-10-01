package com.sp.api.payment.dto;

/** 결제창을 열 때 그대로 넘길 주문 정보. 금액은 서버가 정한 값이다. */
public record MembershipOrderResponse(
        String orderId,
        String orderName,
        int amount,
        String customerEmail,
        String customerName
) {
}
