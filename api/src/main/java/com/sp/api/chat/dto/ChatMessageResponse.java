package com.sp.api.chat.dto;

import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.vtuber.dto.OshiMark;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long liveId,
        Long userId,
        String nickname,
        String content,
        /** 이 방송의 채널을 구독한 사람에게만 붙는 표식. 아니면 null. */
        String oshiMarkUrl,
        /** 표식이 있을 때 구독 등급(BASIC / PAID). 없으면 null. */
        String oshiTier,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return from(message, null);
    }

    public static ChatMessageResponse from(ChatMessage message, OshiMark mark) {
        return new ChatMessageResponse(
                message.getId(),
                message.getLiveStream().getId(),
                message.getUser().getId(),
                message.getUser().getNickname(),
                message.getContent(),
                mark == null ? null : mark.url(),
                mark == null ? null : mark.tier().name(),
                message.getCreatedAt()
        );
    }
}
