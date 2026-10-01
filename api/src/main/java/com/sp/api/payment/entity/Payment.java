package com.sp.api.payment.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 유료 구독 결제 한 건.
 *
 * 주문(READY)은 결제창을 열기 전에 서버가 만든다. 금액을 서버가 정해 두고,
 * 결제 후 돌아온 값과 맞는지 비교하려는 것이다. 돈이 오간 기록이라 지우지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "payments",
        indexes = @Index(name = "idx_payments_user", columnList = "user_id, id")
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토스에 보내는 주문번호. 6~64자, 영문·숫자·-_= 만 쓸 수 있다. */
    @Column(nullable = false, unique = true, length = 64)
    private String orderId;

    /** 승인 뒤에 토스가 주는 결제 키. 취소·조회에 쓴다. 승인 전에는 비어 있다. */
    @Column(unique = true, length = 200)
    private String paymentKey;

    /** 결제한 사람. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 구독 대상 채널. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private User channel;

    /** 서버가 정한 금액(원). */
    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.READY;

    /** 카드 · 간편결제 · 계좌이체 등 토스가 알려 주는 결제수단 이름. */
    @Column(length = 50)
    private String method;

    @Column(length = 500)
    private String receiptUrl;

    private LocalDateTime approvedAt;

    @Column(length = 100)
    private String failureCode;

    @Column(length = 500)
    private String failureMessage;

    private LocalDateTime canceledAt;

    @Column(length = 200)
    private String cancelReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Payment(String orderId, User user, User channel, int amount) {
        this.orderId = orderId;
        this.user = user;
        this.channel = channel;
        this.amount = amount;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void markDone(String paymentKey, String method, String receiptUrl, LocalDateTime approvedAt) {
        this.status = PaymentStatus.DONE;
        this.paymentKey = paymentKey;
        this.method = method;
        this.receiptUrl = receiptUrl;
        this.approvedAt = approvedAt == null ? LocalDateTime.now() : approvedAt;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void markFailed(String code, String message) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = truncate(code, 100);
        this.failureMessage = truncate(message, 500);
    }

    public void markCanceled(String reason, LocalDateTime at) {
        this.status = PaymentStatus.CANCELED;
        this.canceledAt = at;
        this.cancelReason = truncate(reason, 200);
    }

    /** 승인된 뒤 days 일이 아직 안 지났는지. 사용자 환불이 가능한 기간이다. */
    public boolean isWithinRefundWindow(int days, LocalDateTime now) {
        return approvedAt != null && approvedAt.plusDays(days).isAfter(now);
    }

    public boolean isDone() {
        return status == PaymentStatus.DONE;
    }

    public boolean isOwnedBy(String email) {
        return user.getEmail().equals(email);
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
