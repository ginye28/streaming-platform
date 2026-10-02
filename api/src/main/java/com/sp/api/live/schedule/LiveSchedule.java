package com.sp.api.live.schedule;

import com.sp.api.live.entity.Audience;
import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 방송 예약. 방송 전에 "언제 방송한다" 를 올려 두면 구독자에게 알림이 가고, 시청자는 대기실에서 기다린다.
 * 방송이 시작되면 이 예약이 방송(live_stream_id)으로 이어지고, 예약에 적어 둔 제목·설명·공개 대상이 방송에 쓰인다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "live_schedules", indexes = {
        @Index(name = "idx_live_schedules_status", columnList = "status, scheduledAt"),
        @Index(name = "idx_live_schedules_user", columnList = "user_id, status")
})
public class LiveSchedule {

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

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Audience audience = Audience.ALL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Audience chatAudience = Audience.ALL;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Status status = Status.SCHEDULED;

    /** 이 예약으로 시작된 방송. 아직이면 null. */
    private Long liveStreamId;

    /** "곧 시작합니다" 알림을 보낸 때. 시간이 바뀌면 다시 보내도록 비운다. */
    private LocalDateTime reminderSentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LiveSchedule(User user, String title, String description, String thumbnailUrl,
                        LocalDateTime scheduledAt, Audience audience, Audience chatAudience) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.scheduledAt = scheduledAt;
        this.audience = Audience.orAll(audience);
        this.chatAudience = Audience.orAll(chatAudience);
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** @return 방송 시각이 바뀌었는지 */
    public boolean update(String title, String description, String thumbnailUrl,
                          LocalDateTime scheduledAt, Audience audience, Audience chatAudience) {

        boolean timeChanged = !this.scheduledAt.equals(scheduledAt);

        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.scheduledAt = scheduledAt;
        this.audience = Audience.orAll(audience);
        this.chatAudience = Audience.orAll(chatAudience);

        if (timeChanged) {
            this.reminderSentAt = null;
        }

        return timeChanged;
    }

    public boolean isScheduled() {
        return status == Status.SCHEDULED;
    }

    public boolean isOwnedBy(Long userId) {
        return user.getId().equals(userId);
    }

    public void cancel() {
        this.status = Status.CANCELED;
    }

    public void markStarted(Long liveStreamId) {
        this.status = Status.STARTED;
        this.liveStreamId = liveStreamId;
    }

    /** 시각이 한참 지나도 방송하지 않아 목록에서 내린다. */
    public void expire() {
        this.status = Status.EXPIRED;
    }

    public void markReminderSent() {
        this.reminderSentAt = LocalDateTime.now();
    }

    public enum Status {
        /** 방송 전. */
        SCHEDULED,
        /** 방송이 시작됐다. */
        STARTED,
        /** 주인이 취소했다. */
        CANCELED,
        /** 방송하지 않은 채 시각이 한참 지났다. */
        EXPIRED
    }
}
