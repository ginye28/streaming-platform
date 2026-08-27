package com.sp.api.chat.dto;

import com.sp.api.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long liveId,
        Long userId,
        String nickname,
        String content,
        /** 이 방송의 채널을 구독한 사람에게만 붙는 표식. 아니면 null. */
        String oshiMarkUrl,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return from(message, null);
    }

    public static ChatMessageResponse from(ChatMessage message, String oshiMarkUrl) {
        return new ChatMessageResponse(
                message.getId(),
                message.getLiveStream().getId(),
                message.getUser().getId(),
                message.getUser().getNickname(),
                message.getContent(),
                oshiMarkUrl,
                message.getCreatedAt()
        );
    }
}
