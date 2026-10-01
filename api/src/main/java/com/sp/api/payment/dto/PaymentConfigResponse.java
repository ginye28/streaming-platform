package com.sp.api.payment.dto;

/**
 * 화면이 결제 흐름을 열지 말지 정하는 데 쓰는 설정.
 * enabled 가 false 면 결제가 꺼져 있다는 뜻이고, clientKey 는 null 이다.
 */
public record PaymentConfigResponse(
        boolean enabled,
        String clientKey,
        int priceKrw,
        int periodDays
) {
}
