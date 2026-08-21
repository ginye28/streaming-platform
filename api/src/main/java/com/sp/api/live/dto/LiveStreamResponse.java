package com.sp.api.live.dto;

import com.sp.api.live.entity.LiveStream;

import java.time.LocalDateTime;

public record LiveStreamResponse(
        Long id,
        Long channelId,
        String nickname,
        String title,
        String description,
        String thumbnailUrl,
        /** 재생 주소. 송출 키가 아니라 공개 이름 기반이다. */
        String hlsUrl,
        String status,
        long viewerCount,
        long peakViewerCount,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {

    public static LiveStreamResponse of(LiveStream live, String hlsBaseUrl, long viewerCount) {
        return new LiveStreamResponse(
                live.getId(),
                live.getUser().getId(),
                live.getUser().getNickname(),
                live.getTitle(),
                live.getDescription(),
                live.getThumbnailUrl(),
                hlsBaseUrl + "/" + live.getStreamName() + ".m3u8",
                live.getStatus().name(),
                viewerCount,
                live.getPeakViewerCount(),
                live.getStartedAt(),
                live.getEndedAt()
        );
    }
}
