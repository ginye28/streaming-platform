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

    public ChatMessage(LiveStream liveStream, User user, String content) {
        this.liveStream = liveStream;
        this.user = user;
        this.content = content;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
