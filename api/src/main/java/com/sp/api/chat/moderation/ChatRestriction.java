package com.sp.api.chat.moderation;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채널에서 채팅이 막힌 사람. 일시 정지(restrictedUntil 이 있음)와 강퇴(없음)를 한 표에 둔다.
 * 채널마다 한 사람에 한 행이라서, 다시 제한하면 같은 행을 고친다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_restrictions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel_id", "user_id"})
)
public class ChatRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private User channel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 이 시각까지 막힌다. 비어 있으면 풀어 줄 때까지(강퇴). */
    private LocalDateTime restrictedUntil;

    @Column(length = 100)
    private String reason;

    /** 제한한 사람(주인 또는 매니저)의 사용자 id. */
    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ChatRestriction(User channel, User user, LocalDateTime restrictedUntil, String reason, Long createdBy) {
        this.channel = channel;
        this.user = user;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        restrict(restrictedUntil, reason, createdBy);
    }

    /** 이미 있는 제한을 새 값으로 바꾼다. */
    public void restrict(LocalDateTime restrictedUntil, String reason, Long createdBy) {
        this.restrictedUntil = restrictedUntil;
        this.reason = reason == null || reason.isBlank() ? null : reason.trim();
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isPermanent() {
        return restrictedUntil == null;
    }

    /** 지금도 막혀 있는지. 일시 정지 시간이 지났으면 행이 남아 있어도 막히지 않는다. */
    public boolean isActive(LocalDateTime now) {
        return restrictedUntil == null || restrictedUntil.isAfter(now);
    }
}
