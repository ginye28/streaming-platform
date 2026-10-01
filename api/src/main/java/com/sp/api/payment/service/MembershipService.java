package com.sp.api.payment.service;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.exception.ServiceUnavailableException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.payment.config.PaymentProperties;
import com.sp.api.payment.dto.ConfirmPaymentRequest;
import com.sp.api.payment.dto.MembershipOrderResponse;
import com.sp.api.payment.dto.MembershipResultResponse;
import com.sp.api.payment.dto.PaymentConfigResponse;
import com.sp.api.payment.dto.PaymentHistoryResponse;
import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.gateway.GatewayPayment;
import com.sp.api.payment.gateway.PaymentGateway;
import com.sp.api.payment.gateway.PaymentGatewayException;
import com.sp.api.payment.repository.PaymentRepository;
import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.entity.SubscriptionTier;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 유료 구독 결제.
 *
 * 흐름은 세 걸음이다.
 * 1. 주문 — 서버가 금액을 정해 두고 주문(READY)을 만든다.
 * 2. 결제창 — 브라우저가 토스 결제창을 열어 사용자가 결제한다. 우리 서버는 관여하지 않는다.
 * 3. 승인 — 결제창에서 돌아오면 서버가 금액이 주문과 같은지 확인하고 토스에 승인을 요청한다.
 *    승인이 끝나면 구독 기간을 늘린다. 이 걸음이 끝나야 비로소 돈이 빠져나간다.
 *
 * 승인은 토스를 부르는 동안 DB 트랜잭션을 붙들고 있지 않도록 세 토막으로 나눴다
 * (주문 읽기 → 승인 요청 → 결과 반영). 결과 반영은 주문 행을 잠가서 한 번만 일어난다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final PaymentProperties properties;
    private final PaymentGateway gateway;
    private final PaymentRepository paymentRepository;
    private final SubscribeRepository subscribeRepository;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;

    public PaymentConfigResponse config() {
        boolean enabled = properties.isEnabled();

        return new PaymentConfigResponse(
                enabled,
                enabled ? properties.getClientKey() : null,
                properties.getPriceKrw(),
                properties.getPeriodDays()
        );
    }

    /** 1단계 — 주문을 만든다. 금액은 여기서 정해지고 이후로는 바뀌지 않는다. */
    public MembershipOrderResponse createOrder(Long channelId, String email) {

        requireEnabled();

        return inTransaction(() -> {

            User user = findUser(email);

            User channel = userRepository.findById(channelId)
                    .orElseThrow(() -> new NotFoundException("채널을 찾을 수 없습니다."));

            if (user.getId().equals(channel.getId())) {
                throw new BadRequestException("자기 자신의 채널은 구독할 수 없습니다.");
            }

            // 토스 주문번호는 6~64자, 영문·숫자·-_= 만 쓸 수 있다. 추측할 수 없도록 무작위 값을 붙인다.
            String orderId = "sub-" + channelId + "-" + UUID.randomUUID().toString().replace("-", "");

            paymentRepository.save(new Payment(orderId, user, channel, properties.getPriceKrw()));

            return new MembershipOrderResponse(
                    orderId,
                    channel.getNickname() + " 유료 구독 " + properties.getPeriodDays() + "일",
                    properties.getPriceKrw(),
                    user.getEmail(),
                    user.getNickname()
            );
        });
    }

    /** 3단계 — 결제창에서 돌아온 값을 확인하고 승인한 뒤 구독 기간을 늘린다. */
    public MembershipResultResponse confirm(String email, ConfirmPaymentRequest request) {

        requireEnabled();

        // ① 주문을 읽고, 이 사람의 주문이고 금액이 맞는지 본다. 토스를 부르기 전에 걸러낸다.
        Payment order = inTransaction(() -> {

            Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

            if (!payment.isOwnedBy(email)) {
                throw new ForbiddenException("내 주문이 아닙니다.");
            }

            if (payment.getAmount() != request.getAmount()) {
                throw new BadRequestException("결제 금액이 주문과 다릅니다.");
            }

            return payment;
        });

        // 이미 끝난 주문이면 다시 승인하지 않고 지난 결과를 돌려준다. 새로고침이나 재시도에서 두 번 청구되지 않는다.
        if (order.isDone()) {
            return sameKey(order, request) ? resultOf(order) : fail("이미 다른 결제로 처리된 주문입니다.");
        }

        if (order.getStatus() == PaymentStatus.FAILED) {
            throw new BadRequestException("실패한 주문입니다. 다시 결제해 주세요.");
        }

        // ② 토스에 승인을 요청한다. 트랜잭션 밖이다.
        GatewayPayment approved = approve(order, request);

        // ③ 결과를 반영한다. 주문 행을 잠가 같은 주문이 동시에 두 번 반영되지 않게 한다.
        return inTransaction(() -> apply(request.getOrderId(), approved));
    }

    /** 내 결제 내역. 완료된 결제만. */
    public PageResponse<PaymentHistoryResponse> history(String email, Pageable pageable) {

        User user = findUser(email);

        return PageResponse.from(
                paymentRepository.findByUserIdAndStatusOrderByIdDesc(user.getId(), PaymentStatus.DONE, pageable)
                        .map(PaymentHistoryResponse::from)
        );
    }

    // ---- 승인 ----

    private GatewayPayment approve(Payment order, ConfirmPaymentRequest request) {

        try {
            GatewayPayment result = gateway.confirm(
                    request.getPaymentKey(), request.getOrderId(), request.getAmount());

            return verified(order, request, result);

        } catch (PaymentGatewayException e) {

            // 이미 승인된 결제라는 답이 오면 지난 승인 응답을 놓친 것이다. 결제를 조회해 확인한다.
            if ("ALREADY_PROCESSED_PAYMENT".equals(e.getCode())) {
                return recheck(order, request);
            }

            // 결제사에 닿지 못했거나 결제사 쪽 문제면 승인됐는지 알 수 없다. 실패로 확정하지 않고 다시 시도하게 둔다.
            if (e.isRetryable()) {
                throw new ServiceUnavailableException(e.getMessage());
            }

            markFailed(request.getOrderId(), e.getCode(), e.getMessage());

            throw new BadRequestException(e.getMessage());
        }
    }

    private GatewayPayment recheck(Payment order, ConfirmPaymentRequest request) {

        try {
            return verified(order, request, gateway.find(request.getPaymentKey()));

        } catch (PaymentGatewayException e) {
            throw new ServiceUnavailableException(e.getMessage());
        }
    }

    /** 결제사 응답이 우리가 만든 주문과 정말 같은지 확인한다. 하나라도 다르면 구독을 주지 않는다. */
    private GatewayPayment verified(Payment order, ConfirmPaymentRequest request, GatewayPayment result) {

        boolean sameOrder = request.getOrderId().equals(result.orderId())
                && request.getPaymentKey().equals(result.paymentKey());

        if (!result.isDone() || !sameOrder || result.totalAmount() != order.getAmount()) {

            log.error("결제 응답이 주문과 맞지 않습니다 orderId={} status={}", order.getOrderId(), result.status());

            markFailed(order.getOrderId(), "MISMATCH", "결제 결과가 주문과 맞지 않습니다.");

            throw new BadRequestException("결제 결과가 주문과 맞지 않습니다. 고객센터에 문의해 주세요.");
        }

        return result;
    }

    private MembershipResultResponse apply(String orderId, GatewayPayment approved) {

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("주문을 찾을 수 없습니다."));

        // 잠금을 기다리는 동안 다른 요청이 먼저 반영했다면 기간을 또 늘리지 않는다.
        if (payment.isDone()) {
            return resultOf(payment);
        }

        payment.markDone(
                approved.paymentKey(), approved.method(), approved.receiptUrl(), approved.approvedAt());

        // 구독하지 않은 채 결제했다면 이 결제로 구독도 함께 시작한다.
        Subscribe subscribe = subscribeRepository
                .findBySubscriberIdAndChannelId(payment.getUser().getId(), payment.getChannel().getId())
                .orElseGet(() -> subscribeRepository.save(
                        new Subscribe(payment.getUser(), payment.getChannel())));

        subscribe.grantPaid(LocalDateTime.now(), properties.getPeriodDays());

        return resultOf(payment, subscribe);
    }

    private void markFailed(String orderId, String code, String message) {

        inTransaction(() -> {
            paymentRepository.findByOrderId(orderId)
                    .filter(payment -> !payment.isDone())
                    .ifPresent(payment -> payment.markFailed(code, message));
            return null;
        });
    }

    // ---- 도우미 ----

    private MembershipResultResponse resultOf(Payment payment) {

        return inTransaction(() -> {
            Subscribe subscribe = subscribeRepository
                    .findBySubscriberIdAndChannelId(payment.getUser().getId(), payment.getChannel().getId())
                    .orElse(null);

            return resultOf(payment, subscribe);
        });
    }

    private MembershipResultResponse resultOf(Payment payment, Subscribe subscribe) {

        LocalDateTime now = LocalDateTime.now();

        return new MembershipResultResponse(
                payment.getChannel().getId(),
                payment.getChannel().getNickname(),
                subscribe == null ? SubscriptionTier.BASIC.name() : subscribe.effectiveTier(now).name(),
                subscribe == null ? null : subscribe.getPaidUntil(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getReceiptUrl()
        );
    }

    private static boolean sameKey(Payment order, ConfirmPaymentRequest request) {
        return request.getPaymentKey().equals(order.getPaymentKey());
    }

    private static MembershipResultResponse fail(String message) {
        throw new BadRequestException(message);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ServiceUnavailableException("결제가 아직 설정되지 않았습니다.");
        }
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    private <T> T inTransaction(java.util.function.Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }
}
