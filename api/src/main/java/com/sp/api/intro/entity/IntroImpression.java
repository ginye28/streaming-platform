package com.sp.api.intro.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 어떤 시청자가 어떤 채널의 인트로를 보고 무엇을 눌렀는지.
 *
 * 두 가지 일을 한다.
 * 1. 한 번 본 인트로를 다시 띄우지 않는다.
 * 2. PASS 가 쌓인 채널은 "이어보기"에서 건너뛴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "intro_impressions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"viewer_key", "channel_id"}),
        indexes = @Index(name = "idx_intro_impressions_viewer", columnList = "viewer_key, action")
)
public class IntroImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인했으면 계정, 아니면 접속 IP. 조회수 판정과 같은 방식이다. */
    @Column(name = "viewer_key", nullable = false, length = 120)
    private String viewerKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "channel_id", nullable = false)
    private User channel;

    /**
     * 열거형이지만 DB 에는 문자열 칸으로 만든다.
     * ENUM 으로 두면 종류를 하나 늘릴 때 ddl-auto: update 가 칸을 넓혀 주지 못한다.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Action action;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public IntroImpression(String viewerKey, User channel, Action action) {
        this.viewerKey = viewerKey;
        this.channel = channel;
        record(action);
    }

    public void record(Action action) {
        this.action = action;
        this.updatedAt = LocalDateTime.now();
    }

    public enum Action {
        /** 인트로를 끊고 방송을 보러 갔다. */
        SKIP,
        /** 인트로를 끝까지 봤다. */
        WATCHED,
        /** 이 사람은 안 보고 다음으로 넘어갔다. */
        PASS
    }
}
