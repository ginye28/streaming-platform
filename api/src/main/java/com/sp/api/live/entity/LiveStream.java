package com.sp.api.live.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
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

    public LiveStream(User user, String title, String description, String thumbnailUrl, String streamName) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.streamName = streamName;
        this.status = Status.LIVE;
        this.startedAt = LocalDateTime.now();
        this.peakViewerCount = 0L;
    }

    public void end() {
        this.status = Status.ENDED;
        this.endedAt = LocalDateTime.now();
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
