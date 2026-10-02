package com.sp.api.chat.moderation;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 채널 주인이 채팅 운영을 맡긴 사람(매니저). 그 채널의 모든 방송에서 같은 권한을 갖는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "channel_moderators",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel_id", "user_id"})
)
public class ChannelModerator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private User channel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChannelModerator(User channel, User user) {
        this.channel = channel;
        this.user = user;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
