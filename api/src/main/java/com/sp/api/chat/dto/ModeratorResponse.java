package com.sp.api.chat.dto;

import com.sp.api.chat.moderation.ChannelModerator;

import java.time.LocalDateTime;

public record ModeratorResponse(Long userId, String nickname, LocalDateTime createdAt) {

    public static ModeratorResponse from(ChannelModerator moderator) {
        return new ModeratorResponse(
                moderator.getUser().getId(), moderator.getUser().getNickname(), moderator.getCreatedAt());
    }
}
