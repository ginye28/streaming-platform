package com.sp.api.notification.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_recipient", columnList = "recipient_id, isRead")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 알림을 받는 사람. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    /**
     * DB 의 ENUM 이 아니라 문자열 칸으로 만든다.
     * ENUM 으로 두면 종류를 하나 늘릴 때 ddl-auto: update 가 기존 칸을 넓혀 주지 못해,
     * 이미 만들어진 DB 에서 "Value not permitted for column" 으로 터진다.
     * (Hibernate 가 "이 값들만 허용" 검사는 그래도 붙이므로, 종류를 늘릴 때
     *  이미 만들어진 DB 를 손봐야 하는 건 마찬가지다 — README 참고.)
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private Type type;

    @Column(nullable = false, length = 200)
    private String message;

    /** 알림을 눌렀을 때 이동할 대상 채널. */
    private Long channelId;

    /** 알림을 눌렀을 때 이동할 대상(방송) id. */
    private Long targetId;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Notification(User recipient, Type type, String message, Long channelId, Long targetId) {
        this.recipient = recipient;
        this.type = type;
        this.message = message;
        this.channelId = channelId;
        this.targetId = targetId;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public boolean isOwnedBy(Long userId) {
        return recipient.getId().equals(userId);
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Type {
        LIVE_START,
        STREAM_COMMENT,
        COMMENT_REPLY
    }
}
