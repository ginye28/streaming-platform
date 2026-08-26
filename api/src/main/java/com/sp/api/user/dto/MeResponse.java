package com.sp.api.user.dto;

import com.sp.api.user.entity.User;

public record MeResponse(
        Long id,
        String email,
        String nickname,
        String profileImage,
        /** 프론트에서 관리자 메뉴를 보일지 판단하는 데 쓴다. 본인 정보라 노출해도 된다. */
        User.Role role
) {

    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                user.getRole()
        );
    }
}
