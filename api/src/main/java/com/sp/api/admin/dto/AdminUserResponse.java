package com.sp.api.admin.dto;

import com.sp.api.user.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String nickname,
        User.Role role,
        LocalDateTime createdAt
) {

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
