package com.sp.api.payment.gateway;

/**
 * 결제사 호출이 실패했다.
 *
 * @param code      결제사가 준 오류 코드. 못 받았으면 TOSS_UNAVAILABLE.
 * @param retryable 결제사에 닿지 못했거나 결제사 쪽 문제일 때 true. 이때는 승인 여부를 알 수 없으므로
 *                  주문을 실패로 확정하지 않고, 같은 요청을 다시 보낼 수 있게 남겨 둔다.
 */
public class PaymentGatewayException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    public PaymentGatewayException(String code, String message, boolean retryable) {
        super(message);
        this.code = code;
        this.retryable = retryable;
    }

    public String getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
