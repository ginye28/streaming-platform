package com.sp.api.channel.dto;

import com.sp.api.user.entity.User;

public record ChannelResponse(
        Long id,
        String nickname,
        String profileImage,
        long subscriberCount,
        long streamCount,
        /** 요청한 사용자가 구독 중인지. 비로그인이면 항상 false. */
        boolean subscribedByMe
) {

    public static ChannelResponse of(User user, long subscriberCount, long streamCount, boolean subscribedByMe) {
        return new ChannelResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImage(),
                subscriberCount,
                streamCount,
                subscribedByMe
        );
    }
}
