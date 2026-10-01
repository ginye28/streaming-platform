package com.sp.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 결제창에서 돌아올 때 주소에 붙어 오는 값 그대로. 토스가 successUrl 에 붙여 준다. */
@Getter
@Setter
@NoArgsConstructor
public class ConfirmPaymentRequest {

    @NotBlank(message = "paymentKey 가 필요합니다.")
    private String paymentKey;

    @NotBlank(message = "orderId 가 필요합니다.")
    private String orderId;

    @Positive(message = "amount 가 올바르지 않습니다.")
    private long amount;
}
