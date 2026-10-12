package com.sp.api.payment.dto;

/** 결제창을 열 때 그대로 넘길 주문 정보. 금액은 서버가 정한 값이다. */
public record MembershipOrderResponse(
        String orderId,
        String orderName,
        int amount,
        String customerEmail,
        String customerName,
        /** 이 주문으로 늘어나는 이용 기간(일). */
        int periodDays
) {
}
