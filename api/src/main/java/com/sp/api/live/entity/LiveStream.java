package com.sp.api.live.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실시간 방송 1회분.
 *
 * 업로드 영상(Stream)과 분리한 이유: 수명주기(업로드 vs 송출 시작·종료),
 * 정렬 기준(조회수 vs 동시 시청자), 필요한 필드가 서로 다르다.
 * 한 테이블에 합치면 videoUrl 과 hlsUrl 중 하나가 항상 비는 상태가 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "live_streams", indexes = {
        @Index(name = "idx_live_streams_status", columnList = "status"),
        @Index(name = "idx_live_streams_user", columnList = "user_id")
})
public class LiveStream {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailUrl;

    /** HLS 재생에 쓰이는 공개 이름. 송출 키가 아니다. */
    @Column(nullable = false, length = 100)
    private String streamName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    /** 방송 중 기록된 최고 동시 시청자 수. */
    @Column(nullable = false)
    private long peakViewerCount = 0L;

    /**
     * 영상을 볼 수 있는 사람. 제한이 있으면 재생 주소는 볼 수 있는 사람에게만 내려간다.
     * 방송을 시작할 때 정해지고 방송 중에는 바뀌지 않는다(주소가 이미 나갔기 때문이다).
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Audience audience = Audience.ALL;

    /** 채팅을 쓸 수 있는 사람. 방송 중에도 바꿀 수 있다. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Audience chatAudience = Audience.ALL;

    /** 한 사람이 채팅을 다시 보내기까지 기다려야 하는 시간(초). 0 이면 끔. */
    @Column(nullable = false)
    private int slowModeSeconds = 0;

    /** 채팅창 위에 고정한 메시지. 없으면 null. */
    private Long pinnedMessageId;

    /** 이 방송이 다시보기로 남았는지. 스트리밍 서버가 녹화를 켜 둔 경우에만 true. */
    @Column(nullable = false)
    private boolean vodAvailable = false;

    public LiveStream(User user, String title, String description, String thumbnailUrl, String streamName) {
        this(user, title, description, thumbnailUrl, streamName, Audience.ALL, Audience.ALL, 0);
    }

    public LiveStream(User user, String title, String description, String thumbnailUrl, String streamName,
                      Audience audience, Audience chatAudience, int slowModeSeconds) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.streamName = streamName;
        this.status = Status.LIVE;
        this.startedAt = LocalDateTime.now();
        this.peakViewerCount = 0L;
        this.audience = Audience.orAll(audience);
        this.chatAudience = Audience.orAll(chatAudience);
        this.slowModeSeconds = Math.max(0, slowModeSeconds);
    }

    /** @param vodAvailable 스트리밍 서버가 이 방송을 다시보기로 남겼다고 보는지 */
    public void end(boolean vodAvailable) {
        this.status = Status.ENDED;
        this.endedAt = LocalDateTime.now();
        this.vodAvailable = vodAvailable;
        this.pinnedMessageId = null;
    }

    public void changeSlowMode(int seconds) {
        this.slowModeSeconds = Math.max(0, seconds);
    }

    public void changeChatAudience(Audience audience) {
        this.chatAudience = Audience.orAll(audience);
    }

    public void pin(Long messageId) {
        this.pinnedMessageId = messageId;
    }

    public void unpin() {
        this.pinnedMessageId = null;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public boolean isLive() {
        return status == Status.LIVE;
    }

    public void recordViewerCount(long viewerCount) {
        if (viewerCount > peakViewerCount) {
            peakViewerCount = viewerCount;
        }
    }

    public enum Status {
        LIVE, ENDED
    }
}
