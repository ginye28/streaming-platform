package com.sp.api.chat.moderation;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 채널 채팅에서 막을 단어. 비교는 대소문자와 공백을 무시한다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "channel_banned_words",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel_id", "word"})
)
public class ChannelBannedWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private User channel;

    /** 저장할 때 소문자로 바꾸고 앞뒤 공백을 뗀 값. */
    @Column(nullable = false, length = 30)
    private String word;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ChannelBannedWord(User channel, String word) {
        this.channel = channel;
        this.word = word;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
