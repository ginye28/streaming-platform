package com.sp.api.live.schedule;

import com.sp.api.live.entity.Audience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 방송 예약 만들기·고치기 요청.
 *
 * scheduledAt 은 시간대가 붙은 시각이다(예: 2026-10-05T20:00:00+09:00). 시간대 없는 시각으로 받으면
 * 서버가 도는 곳의 시간대(무료 서버는 UTC)로 읽혀서, 한국에서 20시에 한 예약이 9시간 어긋난다.
 */
public record LiveScheduleRequest(
        @NotBlank @Size(max = 100) String title,
        String description,
        String thumbnailUrl,
        @NotNull(message = "방송 시각이 필요합니다.") OffsetDateTime scheduledAt,
        Audience audience,
        Audience chatAudience
) {
}
