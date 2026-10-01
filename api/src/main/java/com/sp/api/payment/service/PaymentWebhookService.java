package com.sp.api.payment.service;

import com.sp.api.common.exception.ServiceUnavailableException;
import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.gateway.GatewayPayment;
import com.sp.api.payment.gateway.PaymentGateway;
import com.sp.api.payment.gateway.PaymentGatewayException;
import com.sp.api.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 토스가 결제 상태 변경을 알려 주는 웹훅(PAYMENT_STATUS_CHANGED).
 *
 * **본문을 믿지 않는다.** 이 주소는 누구나 부를 수 있으므로, 받은 값은 "어느 주문을 다시 보라" 는
 * 신호로만 쓰고 실제 상태는 토스에 직접 조회해서 정한다. 그래서 가짜 웹훅을 보내도 할 수 있는 일은
 * 우리 서버가 토스를 한 번 조회하게 만드는 것뿐이다.
 *
 * 하는 일은 두 가지다.
 * 1. 우리는 주문(READY)인데 토스에서는 승인(DONE)됐다 — 결제창에서 돌아오기 전에 연결이 끊긴 경우.
 *    승인 결과를 반영해 유료 구독을 시작한다.
 * 2. 우리는 승인(DONE)인데 토스에서는 취소(CANCELED)됐다 — 상점관리자에서 직접 취소한 경우.
 *    우리 쪽도 취소로 맞추고 늘어난 유료 기간을 되돌린다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private final PaymentGateway gateway;
    private final PaymentRepository paymentRepository;
    private final MembershipService membershipService;
    private final PaymentRefundService refundService;

    public void handle(Map<String, Object> payload) {

        if (payload == null || !"PAYMENT_STATUS_CHANGED".equals(text(payload.get("eventType")))) {
            return;
        }

        if (!(payload.get("data") instanceof Map<?, ?> data)) {
            return;
        }

        String orderId = text(data.get("orderId"));
        String paymentKey = text(data.get("paymentKey"));

        if (orderId == null || paymentKey == null) {
            return;
        }

        // 우리 주문이 아니면 무시한다. 오류로 돌려주면 토스가 계속 다시 보낸다.
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        if (payment == null) {
            log.info("웹훅: 모르는 주문 orderId={}", orderId);
            return;
        }

        try {
            if (payment.getStatus() == PaymentStatus.READY) {
                reconcileApproval(payment, paymentKey);
            } else if (payment.getStatus() == PaymentStatus.DONE) {
                reconcileCancellation(payment);
            }

        } catch (PaymentGatewayException e) {
            // 토스에 닿지 못했다. 503 으로 돌려주면 토스가 나중에 다시 보낸다.
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    private void reconcileApproval(Payment payment, String paymentKey) {

        GatewayPayment actual = gateway.find(paymentKey);

        boolean matches = actual.isDone()
                && payment.getOrderId().equals(actual.orderId())
                && paymentKey.equals(actual.paymentKey())
                && actual.totalAmount() == payment.getAmount();

        if (matches) {
            log.info("웹훅: 승인 확인 orderId={}", payment.getOrderId());
            membershipService.applyApproved(payment.getOrderId(), actual);
        }
    }

    private void reconcileCancellation(Payment payment) {

        // 웹훅이 준 결제 키가 아니라 우리가 저장해 둔 결제 키로 조회한다.
        GatewayPayment actual = gateway.find(payment.getPaymentKey());

        if (actual.isCanceled()) {
            log.info("웹훅: 취소 확인 orderId={}", payment.getOrderId());
            refundService.applyCancellation(payment.getOrderId(), "토스에서 결제가 취소되었습니다.");
        }
    }

    private static String text(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }
}
