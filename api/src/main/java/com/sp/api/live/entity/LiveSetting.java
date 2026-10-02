package com.sp.api.live.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 다음 방송에 사용할 제목·설명·썸네일.
 * OBS 는 제목을 보내주지 않으므로 미리 저장해 두고 송출 시작 시 가져다 쓴다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "live_settings")
public class LiveSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnailUrl;

    /** 다음 방송을 볼 수 있는 사람. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Audience audience = Audience.ALL;

    /** 다음 방송에서 채팅을 쓸 수 있는 사람. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Audience chatAudience = Audience.ALL;

    /** 다음 방송을 슬로우 모드로 시작할 때의 대기 시간(초). 0 이면 끔. */
    @Column(nullable = false)
    private int slowModeSeconds = 0;

    public LiveSetting(User user, String title, String description, String thumbnailUrl) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void update(String title, String description, String thumbnailUrl,
                       Audience audience, Audience chatAudience, int slowModeSeconds) {
        this.title = title;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.audience = Audience.orAll(audience);
        this.chatAudience = Audience.orAll(chatAudience);
        this.slowModeSeconds = Math.max(0, slowModeSeconds);
    }
}
