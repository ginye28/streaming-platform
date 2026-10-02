package com.sp.api.chat.dto;

import com.sp.api.chat.moderation.ChatRestriction;

import java.time.LocalDateTime;

/** 채팅이 막힌 사람. restrictedUntil 이 없으면 강퇴(풀어 줄 때까지)다. */
public record RestrictionResponse(
        Long userId,
        String nickname,
        LocalDateTime restrictedUntil,
        boolean permanent,
        String reason,
        LocalDateTime createdAt
) {

    public static RestrictionResponse from(ChatRestriction restriction) {
        return new RestrictionResponse(
                restriction.getUser().getId(),
                restriction.getUser().getNickname(),
                restriction.getRestrictedUntil(),
                restriction.isPermanent(),
                restriction.getReason(),
                restriction.getCreatedAt()
        );
    }
}
