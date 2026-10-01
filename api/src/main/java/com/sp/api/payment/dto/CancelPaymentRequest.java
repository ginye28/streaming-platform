package com.sp.api.payment.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 환불 요청. 사유는 비워 둘 수 있고(사용자), 토스 취소 사유에도 그대로 들어간다. */
@Getter
@Setter
@NoArgsConstructor
public class CancelPaymentRequest {

    @Size(max = 200, message = "사유는 200자 이하여야 합니다.")
    private String reason;
}
