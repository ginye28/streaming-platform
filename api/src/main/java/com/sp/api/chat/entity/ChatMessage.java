package com.sp.api.chat.entity;

import com.sp.api.live.entity.LiveStream;
import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_messages_live", columnList = "live_stream_id, id")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "live_stream_id", nullable = false)
    private LiveStream liveStream;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 후원(슈퍼챗)이면 후원 금액(원). 일반 채팅이면 null. */
    private Integer donationAmount;

    /** 후원 메시지가 나온 결제. 환불되면 이 값으로 메시지를 찾아 지운다. */
    private Long donationPaymentId;

    /**
     * 지운 메시지. 행은 남기고 이 표시만 켠다 — 신고 기록과 다시보기의 순서가 어긋나지 않게 하려는 것이다.
     * 지운 메시지의 내용은 어느 응답에도 내려가지 않는다.
     */
    @Column(nullable = false)
    private boolean deleted = false;

    public ChatMessage(LiveStream liveStream, User user, String content) {
        this.liveStream = liveStream;
        this.user = user;
        this.content = content;
    }

    /** 후원 메시지. 남긴 말이 없어도 만들 수 있어 content 는 빈 문자열일 수 있다. */
    public static ChatMessage donation(
            LiveStream liveStream, User user, String content, int amount, Long paymentId) {

        ChatMessage message = new ChatMessage(liveStream, user, content == null ? "" : content);
        message.donationAmount = amount;
        message.donationPaymentId = paymentId;

        return message;
    }

    public boolean isDonation() {
        return donationAmount != null;
    }

    public void delete() {
        this.deleted = true;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
