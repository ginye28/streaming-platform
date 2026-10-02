package com.sp.api.chat.dto;

import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.chat.moderation.ChatRole;
import com.sp.api.vtuber.dto.OshiMark;

import java.time.Duration;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long liveId,
        Long userId,
        String nickname,
        /** 지운 메시지는 null. 내용이 어느 응답에도 나가지 않게 한다. */
        String content,
        /** 이 방송의 채널을 구독한 사람에게만 붙는 표식. 아니면 null. */
        String oshiMarkUrl,
        /** 표식이 있을 때 구독 등급(BASIC / PAID). 없으면 null. */
        String oshiTier,
        /** 이 채널에서의 역할(OWNER / MANAGER). 일반 시청자는 null. */
        String role,
        /** 후원이면 금액(원). 일반 채팅은 null. */
        Integer donationAmount,
        /** 후원 단계(1~5). 일반 채팅은 null. */
        Integer donationTier,
        /**
         * 후원 띠가 채팅창 위에 앞으로 더 남아 있어야 하는 시간(초). 방금 온 후원은 단계별 전체 시간이고,
         * 접속하기 전에 온 후원은 그만큼 줄어 있다. 일반 채팅과 다시보기에서는 null.
         */
        Integer donationPinRemainingSeconds,
        boolean deleted,
        /** 다시보기용: 방송을 시작한 뒤 몇 초째 보낸 메시지인지. 실시간 채팅에서는 null. */
        Long offsetSeconds,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return from(message, null, null, null);
    }

    public static ChatMessageResponse from(ChatMessage message, OshiMark mark, ChatRole role, Long offsetSeconds) {

        Integer amount = message.getDonationAmount();
        Integer tier = amount == null ? null : DonationTier.of(amount);

        return new ChatMessageResponse(
                message.getId(),
                message.getLiveStream().getId(),
                message.getUser().getId(),
                message.getUser().getNickname(),
                message.isDeleted() ? null : message.getContent(),
                mark == null ? null : mark.url(),
                mark == null ? null : mark.tier().name(),
                role == null ? null : role.name(),
                amount,
                tier,
                tier == null || offsetSeconds != null ? null : pinRemaining(message, tier),
                message.isDeleted(),
                offsetSeconds,
                message.getCreatedAt()
        );
    }

    private static int pinRemaining(ChatMessage message, int tier) {

        long age = message.getCreatedAt() == null
                ? 0
                : Duration.between(message.getCreatedAt(), LocalDateTime.now()).toSeconds();

        return (int) Math.max(0, DonationTier.pinSeconds(tier) - Math.max(0, age));
    }
}
