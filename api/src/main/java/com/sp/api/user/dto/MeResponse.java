package com.sp.api.user.dto;

import com.sp.api.user.entity.User;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        String profileImage
) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage()
        );
    }
}
