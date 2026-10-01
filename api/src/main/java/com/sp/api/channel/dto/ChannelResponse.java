package com.sp.api.channel.dto;

import com.sp.api.subscribe.entity.SubscriptionTier;
import com.sp.api.user.entity.User;

import java.time.LocalDateTime;

public record ChannelResponse(
        Long id,
        String nickname,
        String profileImage,
        long subscriberCount,
        long streamCount,
        /** 지금 방송 중인지. */
        boolean live,
        /** 요청한 사용자가 구독 중인지. 비로그인이면 항상 false. */
        boolean subscribedByMe,
        /** 내 구독 등급. 구독 중이 아니면 null. */
        SubscriptionTier myTier,
        /** 내 이름 옆에 이 채널의 오시마크를 보일지. 구독 중이 아니면 false. */
        boolean myMarkVisible,
        /** 유료 구독이 끝나는 때. 유료가 아니거나 기한이 없으면 null. */
        LocalDateTime myPaidUntil
) {

    public static ChannelResponse of(
            User user,
            long subscriberCount,
            long streamCount,
            boolean live,
            boolean subscribedByMe,
            SubscriptionTier myTier,
            boolean myMarkVisible,
            LocalDateTime myPaidUntil
    ) {
        return new ChannelResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                subscriberCount,
                streamCount,
                live,
                subscribedByMe,
                myTier,
                myMarkVisible,
                myPaidUntil
        );
    }
}
