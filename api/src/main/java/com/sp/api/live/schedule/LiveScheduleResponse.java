package com.sp.api.live.schedule;

import java.time.OffsetDateTime;
import java.time.ZoneId;

public record LiveScheduleResponse(
        Long id,
        Long channelId,
        String nickname,
        String title,
        String description,
        String thumbnailUrl,
        /** 시간대가 붙은 방송 시각. 브라우저가 바로 자기 시간대로 바꿔 보여 준다. */
        OffsetDateTime scheduledAt,
        String audience,
        String chatAudience,
        /** SCHEDULED(방송 전) / STARTED(시작됨) / CANCELED(취소) / EXPIRED(시각이 한참 지남). */
        String status,
        /** 방송이 시작됐으면 그 방송. 대기실이 이걸 보고 방송으로 넘어간다. */
        Long liveId,
        /** 이 시청자는 이 방송을 볼 수 없다(구독 등급이 모자란다). */
        boolean locked
) {

    public static LiveScheduleResponse of(LiveSchedule schedule, boolean canWatch) {
        return new LiveScheduleResponse(
                schedule.getId(),
                schedule.getUser().getId(),
                schedule.getUser().getNickname(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getThumbnailUrl(),
                schedule.getScheduledAt().atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                schedule.getAudience().name(),
                schedule.getChatAudience().name(),
                schedule.getStatus().name(),
                schedule.getLiveStreamId(),
                !canWatch
        );
    }
}
