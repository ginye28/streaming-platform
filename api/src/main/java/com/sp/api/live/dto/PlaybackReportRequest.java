package com.sp.api.live.dto;

import com.sp.api.live.relay.PlaybackStats;
import jakarta.validation.constraints.NotNull;

/**
 * 시청 화면이 알려 주는 재생 방식. 값은 정해진 목록 안에서만 받아서 아무 글이나 쌓이지 않는다.
 * HLS 일 때만 reason 이 의미 있고, 없으면 OTHER 로 센다.
 */
public record PlaybackReportRequest(
        @NotNull PlaybackStats.Mode mode,
        PlaybackStats.Reason reason
) {
}
