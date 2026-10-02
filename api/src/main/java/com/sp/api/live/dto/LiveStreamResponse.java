package com.sp.api.live.dto;

import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.moderation.ChatRole;
import com.sp.api.live.config.LiveProperties;
import com.sp.api.live.entity.LiveStream;

import java.time.LocalDateTime;

public record LiveStreamResponse(
        Long id,
        Long channelId,
        String nickname,
        String title,
        String description,
        String thumbnailUrl,
        /** 재생 주소. 송출 키가 아니라 공개 이름 기반이다. 볼 수 없는 시청자(locked)에게는 null. */
        String hlsUrl,
        String status,
        long viewerCount,
        long peakViewerCount,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        /** 영상을 볼 수 있는 사람(ALL / SUBSCRIBERS / PAID). */
        String audience,
        /** 채팅을 쓸 수 있는 사람(ALL / SUBSCRIBERS / PAID). */
        String chatAudience,
        /** 슬로우 모드 대기 시간(초). 0 이면 꺼짐. */
        int slowModeSeconds,
        /** 이 시청자는 영상을 볼 수 없다(구독 등급이 모자란다). */
        boolean locked,
        /** 끝난 방송의 다시보기 주소. 다시보기가 없거나 볼 수 없으면 null. */
        String vodUrl,
        /** 채팅창 위에 고정된 메시지. 방송 하나를 조회할 때만 채워진다. */
        ChatMessageResponse pinnedMessage,
        /** 이 채널에서 내 역할(OWNER / MANAGER). 아니거나 방송 하나를 조회한 것이 아니면 null. */
        String myRole,
        /** 채팅 대상(chatAudience) 때문에 이 시청자는 채팅을 쓸 수 없다. 방송 하나를 조회할 때만 채워진다. */
        boolean chatLocked
) {

    public static LiveStreamResponse of(
            LiveStream live, LiveProperties properties, long viewerCount, boolean canWatch,
            ChatMessageResponse pinned, ChatRole myRole, boolean chatLocked) {

        boolean hasVod = !live.isLive() && live.isVodAvailable();

        return new LiveStreamResponse(
                live.getId(),
                live.getUser().getId(),
                live.getUser().getNickname(),
                live.getTitle(),
                live.getDescription(),
                live.getThumbnailUrl(),
                canWatch ? properties.getHlsBaseUrl() + "/" + live.getStreamName() + ".m3u8" : null,
                live.getStatus().name(),
                viewerCount,
                live.getPeakViewerCount(),
                live.getStartedAt(),
                live.getEndedAt(),
                live.getAudience().name(),
                live.getChatAudience().name(),
                live.getSlowModeSeconds(),
                !canWatch,
                canWatch && hasVod ? properties.vodUrlFor(live.getStreamName()) : null,
                pinned,
                myRole == null ? null : myRole.name(),
                chatLocked
        );
    }
}
