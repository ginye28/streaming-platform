package com.sp.api.subscribe.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"subscriber_id", "channel_id"})
        }
)
public class Subscribe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 구독하는 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private User subscriber;

    // 구독받는 사람(채널)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private User channel;

    /** 구독 등급. 처음 구독하면 BASIC 이다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier = SubscriptionTier.BASIC;

    /**
     * 이 채널의 오시마크를 내 이름 옆에 보일지. 구독했다고 해서 마크를 강제로 달지 않는다.
     * 기본은 보임이고, 시청자가 채널마다 끌 수 있다.
     */
    @Column(nullable = false)
    private boolean markVisible = true;

    public Subscribe(User subscriber, User channel) {
        this.subscriber = subscriber;
        this.channel = channel;
    }

    public void changeTier(SubscriptionTier tier) {
        this.tier = tier;
    }

    public void showMark(boolean visible) {
        this.markVisible = visible;
    }
}
