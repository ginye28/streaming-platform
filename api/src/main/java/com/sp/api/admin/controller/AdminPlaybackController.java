package com.sp.api.admin.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.live.ingest.IngestProperties;
import com.sp.api.live.relay.LiveRelay;
import com.sp.api.live.relay.PlaybackStats;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final IngestProperties ingestProperties;

    @GetMapping
    public ResponseEntity<ApiResponse<PlaybackStats.Snapshot>> snapshot() {
        return ResponseEntity.ok(ApiResponse.ok(stats.snapshot(relay.totalViewerCount())));
    }

    @GetMapping("/low-latency")
    public ResponseEntity<ApiResponse<LowLatencySwitch>> lowLatency() {
        return ResponseEntity.ok(ApiResponse.ok(new LowLatencySwitch(ingestProperties.isLowLatencyEnabled())));
    }

    /**
     * 저지연 중계를 켜고 끈다. 서버를 다시 켜지 않아도 되고, 새로 시작하는 방송부터 적용된다.
     * 이미 진행 중인 방송의 중계는 그대로 두며, 서버를 다시 켜면 설정값(INGEST_LOW_LATENCY)으로 돌아간다.
     */
    @PutMapping("/low-latency")
    public ResponseEntity<ApiResponse<LowLatencySwitch>> setLowLatency(@Valid @RequestBody LowLatencySwitchRequest request) {
        ingestProperties.setLowLatencyEnabled(request.enabled());

        return ResponseEntity.ok(ApiResponse.ok(new LowLatencySwitch(ingestProperties.isLowLatencyEnabled())));
    }

    public record LowLatencySwitch(boolean enabled) {
    }

    /** 요청에서는 Boolean(빠지면 400). primitive 는 Jackson 3 에서 빠진 값을 false 로 읽지 못한다. */
    public record LowLatencySwitchRequest(@NotNull Boolean enabled) {
    }
}
