package com.sp.api.payment.dto;

import com.sp.api.payment.config.PaymentProperties;

/** 고를 수 있는 구독 기간 상품 한 가지. */
public record MembershipPlanResponse(
        /** 몇 개월짜리인지. 주문을 만들 때 이 값을 보낸다. */
        int months,
        /** 이 상품으로 늘어나는 이용 기간(일). */
        int days,
        /** 서버가 정한 결제 금액(원). */
        int amountKrw,
        /** 한 달씩 사는 것보다 얼마나 싼지(%). 0 이면 할인 없음. */
        int discountPercent
) {

    public static MembershipPlanResponse from(PaymentProperties.Plan plan) {
        return new MembershipPlanResponse(plan.months(), plan.days(), plan.amountKrw(), plan.discountPercent());
    }
}
