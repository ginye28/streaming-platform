package com.sp.api.chat.dto;

import com.sp.api.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long liveId,
        Long userId,
        String nickname,
        String content,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getLiveStream().getId(),
                message.getUser().getId(),
                message.getUser().getNickname(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
