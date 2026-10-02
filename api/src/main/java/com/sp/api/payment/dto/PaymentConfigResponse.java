package com.sp.api.payment.dto;

import java.util.List;

/**
 * 화면이 결제 흐름을 열지 말지 정하는 데 쓰는 설정.
 * enabled 가 false 면 결제가 꺼져 있다는 뜻이고, clientKey 는 null 이다.
 */
public record PaymentConfigResponse(
        boolean enabled,
        String clientKey,
        int priceKrw,
        int periodDays,
        /** 후원으로 고를 수 있는 금액(원). */
        List<Integer> donationAmounts,
        /** 후원과 함께 남길 수 있는 말의 최대 길이. */
        int donationMessageMaxLength
) {
}
