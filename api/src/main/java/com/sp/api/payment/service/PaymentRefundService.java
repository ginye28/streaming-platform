package com.sp.api.payment.service;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.exception.ServiceUnavailableException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.payment.config.PaymentProperties;
import com.sp.api.payment.dto.AdminPaymentResponse;
import com.sp.api.payment.dto.PaymentHistoryResponse;
import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.gateway.GatewayPayment;
import com.sp.api.payment.gateway.PaymentGateway;
import com.sp.api.payment.gateway.PaymentGatewayException;
import com.sp.api.payment.repository.PaymentRepository;
import com.sp.api.subscribe.repository.SubscribeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 결제 취소(환불).
 *
 * - 사용자: 승인 뒤 환불 가능 기간(기본 7일) 안에, 내 결제만.
 * - 관리자: 기간과 상관없이 어느 결제든. 사유가 필요하다.
 *
 * 환불하면 그 결제로 늘어났던 유료 기간(periodDays)만큼을 되돌린다. 남은 유료 기간이 그보다 짧으면
 * 일반 구독으로 내려간다. 토스에는 전액 취소만 요청한다(부분 환불은 없다).
 *
 * 승인과 같은 모양으로 세 토막이다: 확인(트랜잭션) → 토스 취소 요청 → 결과 반영(주문 행 잠금).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentRefundService {

    private final PaymentProperties properties;
    private final PaymentGateway gateway;
    private final PaymentRepository paymentRepository;
    private final SubscribeRepository subscribeRepository;
    private final DonationService donationService;
    private final PlatformTransactionManager transactionManager;

    /** 사용자가 내 결제를 환불한다. */
    public PaymentHistoryResponse cancelByUser(String email, Long paymentId, String reason) {

        Payment payment = inTransaction(() -> {
            Payment found = find(paymentId);

            if (!found.isOwnedBy(email)) {
                throw new ForbiddenException("내 결제가 아닙니다.");
            }

            // 후원은 이미 방송에 전달된 메시지라, 직접 환불하면 받고 돌려받는 일이 쉬워진다. 관리자가 사정을 보고 처리한다.
            if (found.isDonation()) {
                throw new BadRequestException("후원은 직접 환불할 수 없습니다. 문의해 주세요.");
            }

            requireRefundable(found, true);

            return found;
        });

        cancel(payment, blankTo(reason, "고객 요청 환불"));

        return inTransaction(() -> PaymentHistoryResponse.from(
                find(paymentId), properties.getRefundWindowDays(), LocalDateTime.now()));
    }

    /** 관리자가 어느 결제든 환불한다. 환불 가능 기간은 적용하지 않는다. */
    public AdminPaymentResponse cancelByAdmin(Long paymentId, String reason) {

        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("관리자 환불에는 사유가 필요합니다.");
        }

        Payment payment = inTransaction(() -> {
            Payment found = find(paymentId);
            requireRefundable(found, false);
            return found;
        });

        cancel(payment, reason.trim());

        return inTransaction(() -> AdminPaymentResponse.from(find(paymentId)));
    }

    /** 관리자 결제 목록. status 가 없으면 전체. */
    public PageResponse<AdminPaymentResponse> findForAdmin(PaymentStatus status, Pageable pageable) {

        var page = status == null
                ? paymentRepository.findAllByOrderByIdDesc(pageable)
                : paymentRepository.findByStatusOrderByIdDesc(status, pageable);

        return PageResponse.from(page.map(AdminPaymentResponse::from));
    }

    /**
     * 결제가 취소됐다는 사실을 우리 쪽에 반영한다. 사용자·관리자 환불과 웹훅이 같이 쓴다.
     * 이미 취소된 주문이면 아무 일도 하지 않는다(두 번 되돌리지 않는다).
     */
    public void applyCancellation(String orderId, String reason) {

        inTransaction(() -> {

            Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                    .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

            if (payment.getStatus() != PaymentStatus.DONE) {
                return null;
            }

            payment.markCanceled(reason, LocalDateTime.now());

            if (payment.isDonation()) {
                // 후원은 구독 기간이 없다. 대신 채팅에 올라간 후원 메시지를 지운다.
                donationService.onRefunded(payment);
                return null;
            }

            // 그 결제로 늘어난 기간만큼 되돌린다.
            subscribeRepository
                    .findBySubscriberIdAndChannelId(payment.getUser().getId(), payment.getChannel().getId())
                    .ifPresent(subscribe -> subscribe.revokePaid(LocalDateTime.now(), properties.getPeriodDays()));

            return null;
        });
    }

    // ---- 토스 취소 ----

    private void cancel(Payment payment, String reason) {

        try {
            GatewayPayment result = gateway.cancel(payment.getPaymentKey(), reason);

            if (!result.isCanceled()) {
                log.error("취소 응답이 취소 상태가 아닙니다 orderId={} status={}", payment.getOrderId(), result.status());
                throw new BadRequestException("환불을 완료하지 못했습니다. 잠시 뒤 다시 시도해 주세요.");
            }

        } catch (PaymentGatewayException e) {

            // 이미 취소돼 있다는 답이면 토스 쪽은 끝난 일이다. 우리 쪽만 맞춘다.
            if (!"ALREADY_CANCELED_PAYMENT".equals(e.getCode())) {

                if (e.isRetryable()) {
                    throw new ServiceUnavailableException(e.getMessage());
                }

                // 토스가 거절했다. 결제는 그대로 완료 상태로 둔다.
                throw new BadRequestException(e.getMessage());
            }
        }

        applyCancellation(payment.getOrderId(), reason);
    }

    // ---- 도우미 ----

    private Payment find(Long paymentId) {
        return paymentRepository.findWithUsersById(paymentId)
                .orElseThrow(() -> new NotFoundException("결제를 찾을 수 없습니다."));
    }

    private void requireRefundable(Payment payment, boolean applyWindow) {

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            throw new BadRequestException("이미 환불된 결제입니다.");
        }

        if (payment.getStatus() != PaymentStatus.DONE) {
            throw new BadRequestException("승인된 결제만 환불할 수 있습니다.");
        }

        if (applyWindow && !payment.isWithinRefundWindow(properties.getRefundWindowDays(), LocalDateTime.now())) {
            throw new BadRequestException(
                    "환불 가능 기간(" + properties.getRefundWindowDays() + "일)이 지났습니다. 관리자에게 문의해 주세요.");
        }
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private <T> T inTransaction(Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
}
