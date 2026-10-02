package com.sp.api.payment.service;

import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.chat.event.ChatEvent;
import com.sp.api.chat.event.ChatEventPublisher;
import com.sp.api.chat.moderation.ChatPolicy;
import com.sp.api.chat.repository.ChatMessageRepository;
import com.sp.api.chat.service.ChatMessageAssembler;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.exception.ServiceUnavailableException;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.notification.service.NotificationService;
import com.sp.api.payment.config.PaymentProperties;
import com.sp.api.payment.dto.DonationOrderRequest;
import com.sp.api.payment.dto.MembershipOrderResponse;
import com.sp.api.payment.dto.MembershipResultResponse;
import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentKind;
import com.sp.api.payment.repository.PaymentRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * 방송 후원(슈퍼챗).
 *
 * 결제 흐름은 유료 구독과 같다 — 주문(서버가 금액을 정함) → 토스 결제창 → 승인. 승인과 환불은 MembershipService 와
 * PaymentRefundService 가 하고, 이 클래스는 그 가운데 "후원일 때만 다른 부분" 을 맡는다.
 *
 * - 주문: 방송 중인지, 허용된 금액인지, 이 사람이 지금 채팅할 수 있는지를 결제 전에 확인한다.
 *   채팅할 수 없는 사람이 돈을 낸 뒤에야 막히는 일이 없게 하려는 것이다.
 * - 전달: 승인되면 채팅에 후원 메시지를 올리고 방송 주인에게 알린다.
 * - 환불: 후원 메시지를 채팅에서 지운다. 후원은 사용자가 직접 환불하지 못하고 관리자만 환불한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private final PaymentProperties properties;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPolicy chatPolicy;
    private final ChatMessageAssembler assembler;
    private final ChatEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    /** 1단계 — 후원 주문을 만든다. 돌려받은 값으로 결제창을 연다. */
    public MembershipOrderResponse createOrder(Long liveId, String email, DonationOrderRequest request) {

        if (!properties.isEnabled()) {
            throw new ServiceUnavailableException("결제가 아직 설정되지 않았습니다.");
        }

        return new TransactionTemplate(transactionManager).execute(status -> {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

            LiveStream live = liveStreamRepository.findWithUserById(liveId)
                    .orElseThrow(() -> new NotFoundException("방송을 찾을 수 없습니다."));

            if (!live.isLive()) {
                throw new BadRequestException("방송 중에만 후원할 수 있습니다.");
            }

            if (live.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("자기 방송에는 후원할 수 없습니다.");
            }

            if (!properties.getDonationAmounts().contains(request.amount())) {
                throw new BadRequestException("후원할 수 있는 금액이 아닙니다.");
            }

            // 후원 메시지도 채팅에 올라가므로 채팅과 같은 규칙을 거친다. 슬로우 모드만 적용하지 않는다.
            chatPolicy.check(live, user, request.message(), false);

            // 토스 주문번호는 6~64자, 영문·숫자·-_= 만 쓸 수 있다.
            String orderId = "don-" + liveId + "-" + UUID.randomUUID().toString().replace("-", "");

            paymentRepository.save(Payment.donation(
                    orderId, user, live.getUser(), request.amount(), live, request.message()));

            return new MembershipOrderResponse(
                    orderId,
                    live.getUser().getNickname() + " 방송 후원",
                    request.amount(),
                    user.getEmail(),
                    user.getNickname()
            );
        });
    }

    /**
     * 승인된 후원을 채팅에 올리고 방송 주인에게 알린다. 승인을 반영하는 트랜잭션 안에서 부른다.
     * 같은 결제가 두 번 오지 않도록 호출하는 쪽(MembershipService.apply)이 주문 행을 잠그고 한 번만 부른다.
     */
    public MembershipResultResponse deliver(Payment payment) {

        LiveStream live = payment.getLiveStream();

        ChatMessage message = chatMessageRepository.save(ChatMessage.donation(
                live, payment.getUser(), payment.getDonationMessage(), payment.getAmount(), payment.getId()));

        eventPublisher.message(live.getId(), assembler.assembleOne(live, message));

        notificationService.notifyDonation(payment.getChannel(), payment.getUser(), live, payment.getAmount());

        log.info("후원 전달: paymentId={}, liveId={}, amount={}", payment.getId(), live.getId(), payment.getAmount());

        return resultOf(payment);
    }

    /** 환불된 후원의 채팅 메시지를 지운다. 환불을 반영하는 트랜잭션 안에서 부른다. */
    public void onRefunded(Payment payment) {

        chatMessageRepository.findByDonationPaymentId(payment.getId()).ifPresent(message -> {

            message.delete();

            LiveStream live = message.getLiveStream();

            if (message.getId().equals(live.getPinnedMessageId())) {
                live.unpin();
                eventPublisher.event(live.getId(), ChatEvent.pinned(null));
            }

            eventPublisher.event(live.getId(), ChatEvent.deleted(message.getId()));
        });
    }

    public MembershipResultResponse resultOf(Payment payment) {
        return new MembershipResultResponse(
                payment.getChannel().getId(),
                payment.getChannel().getNickname(),
                null,
                null,
                payment.getAmount(),
                payment.getMethod(),
                payment.getReceiptUrl(),
                PaymentKind.DONATION.name(),
                payment.getLiveStream() == null ? null : payment.getLiveStream().getId()
        );
    }
}
