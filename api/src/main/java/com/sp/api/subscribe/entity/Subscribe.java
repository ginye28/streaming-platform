package com.sp.api.subscribe.entity;

import com.sp.api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    /**
     * 유료 구독이 끝나는 때. 결제로 늘어난다. 비어 있으면 기한이 없다(결제가 꺼져 있을 때의 전환).
     * 기한이 지나면 tier 가 PAID 로 남아 있어도 effectiveTier 는 BASIC 이다.
     */
    private LocalDateTime paidUntil;

    public Subscribe(User subscriber, User channel) {
        this.subscriber = subscriber;
        this.channel = channel;
    }

    public void changeTier(SubscriptionTier tier) {
        this.tier = tier;

        // 일반으로 내리면 남은 유료 기간도 함께 없어진다.
        if (tier == SubscriptionTier.BASIC) {
            this.paidUntil = null;
        }
    }

    /**
     * 결제가 끝났을 때 유료 기간을 days 일 늘린다.
     * 아직 남은 기간이 있으면 그 뒤에 이어 붙이고, 이미 끝났으면 지금부터 센다.
     */
    public void grantPaid(LocalDateTime now, int days) {

        LocalDateTime start = paidUntil != null && paidUntil.isAfter(now) ? paidUntil : now;

        this.tier = SubscriptionTier.PAID;
        this.paidUntil = start.plusDays(days);
    }

    public SubscriptionTier effectiveTier(LocalDateTime now) {
        return SubscriptionTier.effective(tier, paidUntil, now);
    }

    public void showMark(boolean visible) {
        this.markVisible = visible;
    }
}
