package com.sp.api.vtuber.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 채널의 정체성. 방송 제목처럼 매번 바뀌는 것이 아니라 그 사람을 이루는 값들이다.
 *
 * 오시마크와 팬네임은 팬이 자기 소속을 드러내는 수단이고,
 * 데뷔·졸업은 이 바닥에서 무게가 다른 두 날짜다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "channel_profiles")
public class ChannelProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** 팬이 채팅에서 달고 다니는 표식. 이미지를 직접 올린다. */
    private String oshiMarkUrl;

    /** 팬덤 이름. "구독자 1,234명" 대신 "별무리 1,234명" 으로 보인다. */
    @Column(length = 30)
    private String fanName;

    /** 데뷔일. 아직 오지 않았으면 화면에서 남은 날짜를 센다. */
    private LocalDate debutOn;

    /** 졸업일. 넣으면 채널이 졸업한 것으로 표시된다. 올린 영상과 지난 방송은 그대로 남는다. */
    private LocalDate graduatedOn;

    public ChannelProfile(User user) {
        this.user = user;
    }

    public void update(String oshiMarkUrl, String fanName, LocalDate debutOn, LocalDate graduatedOn) {
        this.oshiMarkUrl = blankToNull(oshiMarkUrl);
        this.fanName = blankToNull(fanName);
        this.debutOn = debutOn;
        this.graduatedOn = graduatedOn;
    }

    public boolean isGraduated() {
        return graduatedOn != null && !graduatedOn.isAfter(LocalDate.now());
    }

    /** 데뷔일이 아직 오지 않았으면 남은 날수, 아니면 null. */
    public Long daysUntilDebut() {

        if (debutOn == null || !debutOn.isAfter(LocalDate.now())) {
            return null;
        }

        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), debutOn);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
