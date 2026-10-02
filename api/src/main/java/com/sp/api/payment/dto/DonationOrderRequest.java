package com.sp.api.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 후원 주문 요청. 금액은 서버가 허용한 금액 중에서만 받는다. */
public record DonationOrderRequest(
        @NotNull @Positive Integer amount,
        @Size(max = 100, message = "후원 메시지는 100자 이하여야 합니다.") String message
) {
}
