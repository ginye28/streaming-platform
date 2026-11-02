package com.sp.api.admin.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.live.relay.LiveRelay;
import com.sp.api.live.relay.PlaybackStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 저지연 시청 집계 조회. 인가는 SecurityConfig 에서 ADMIN 으로 제한한다(/api/admin/**).
 * 서버를 재시작하면 0 부터 다시 센다(PlaybackStats).
 */
@RestController
@RequestMapping("/api/admin/playback")
@RequiredArgsConstructor
public class AdminPlaybackController {

    private final PlaybackStats stats;
    private final LiveRelay relay;

    @GetMapping
    public ResponseEntity<ApiResponse<PlaybackStats.Snapshot>> snapshot() {
        return ResponseEntity.ok(ApiResponse.ok(stats.snapshot(relay.totalViewerCount())));
    }
}
