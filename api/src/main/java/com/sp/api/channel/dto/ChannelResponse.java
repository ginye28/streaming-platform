package com.sp.api.channel.dto;

import com.sp.api.user.entity.User;

public record ChannelResponse(
        Long id,
        String nickname,
        String profileImage,
        long subscriberCount,
        long streamCount,
        /** 지금 방송 중인지. */
        boolean live,
        /** 요청한 사용자가 구독 중인지. 비로그인이면 항상 false. */
        boolean subscribedByMe
) {

    public static ChannelResponse of(
            User user,
            long subscriberCount,
            long streamCount,
            boolean live,
            boolean subscribedByMe
    ) {
        return new ChannelResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                subscriberCount,
                streamCount,
                live,
                subscribedByMe
        );
    }
}
