package com.sp.api.block.dto;

import com.sp.api.user.entity.User;

public record BlockedUserResponse(
        Long id,
        String nickname,
        String profileImage
) {

    public static BlockedUserResponse from(User user) {
        return new BlockedUserResponse(user.getId(), user.getNickname(), user.getProfileImage());
    }
}
