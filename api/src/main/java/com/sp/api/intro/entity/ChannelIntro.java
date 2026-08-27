package com.sp.api.intro.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 처음 들어온 시청자에게 보여 줄 자기소개.
 *
 * 방송이 아니라 사람에게 붙는다. 방송마다 제목이 바뀌어도 "이 사람이 누구인가"는
 * 그대로이기 때문이다. 그래서 다음 방송 설정(LiveSetting)과 따로 둔다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "channel_intros")
public class ChannelIntro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** 짧은 자기소개 영상. 없으면 카드로 대신 보여 준다. */
    private String videoUrl;

    /** 한 줄 소개. 카드에서 제일 크게 보이는 문장이다. */
    @Column(length = 60)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String greeting;

    public ChannelIntro(User user, String videoUrl, String headline, String greeting) {
        this.user = user;
        update(videoUrl, headline, greeting);
    }

    public void update(String videoUrl, String headline, String greeting) {
        this.videoUrl = blankToNull(videoUrl);
        this.headline = blankToNull(headline);
        this.greeting = blankToNull(greeting);
    }

    /**
     * 보여 줄 게 하나도 없으면 인트로가 없는 것으로 친다.
     * 빈 인트로를 띄우면 시청자에게는 그냥 걸리적거리는 화면일 뿐이다.
     */
    public boolean isEmpty() {
        return videoUrl == null && headline == null && greeting == null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
