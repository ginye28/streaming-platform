package com.sp.api.payment.gateway;

/**
 * 결제 승인을 대신해 주는 바깥 서비스. 지금 구현은 토스페이먼츠 하나다.
 * 인터페이스로 나눈 것은 테스트에서 진짜 결제 없이 성공·실패를 흉내 내기 위해서다.
 */
public interface PaymentGateway {

    /**
     * 결제를 승인한다. 같은 orderId 로 다시 불러도 한 번만 승인되도록 orderId 를 멱등 키로 쓴다.
     *
     * @throws PaymentGatewayException 승인이 거절됐거나 결제사에 닿지 못했을 때
     */
    GatewayPayment confirm(String paymentKey, String orderId, long amount);

    /** 이미 승인된 결제를 다시 확인한다. 승인 응답을 놓친 경우에 쓴다. */
    GatewayPayment find(String paymentKey);
}
