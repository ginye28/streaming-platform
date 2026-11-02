package com.sp.api.live.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.live.dto.PlaybackReportRequest;
import com.sp.api.live.relay.LiveRelay;
import com.sp.api.live.relay.PlaybackStats;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시청 화면이 저지연 재생을 시작했는지, 일반(HLS)으로 넘어갔다면 왜인지 알려 오는 곳.
 * 로그인 없이 받는다(비로그인 시청자도 있다). 요청 횟수는 일반 쓰기 제한이 막는다.
 * 모인 숫자는 관리자 화면(/api/admin/playback)에서 본다.
 */
@RestController
@RequestMapping("/api/playback")
@RequiredArgsConstructor
public class PlaybackController {

    private final PlaybackStats stats;
    private final LiveRelay relay;

    /**
     * 이 방송을 저지연으로 볼 수 있는가. 시청 화면이 저지연을 시도하기 전에 묻는다
     * (관리자가 저지연을 껐거나 껐을 때 시작된 방송이면 false → 바로 일반 재생).
     */
    @GetMapping("/low-latency")
    public ResponseEntity<ApiResponse<LowLatencyAvailability>> lowLatency(@RequestParam Long liveId) {
        return ResponseEntity.ok(ApiResponse.ok(new LowLatencyAvailability(relay.isOpen(liveId))));
    }

    public record LowLatencyAvailability(boolean available) {
    }

    @PostMapping("/report")
    public ResponseEntity<Void> report(@Valid @RequestBody PlaybackReportRequest request) {

        stats.report(request.mode(), request.reason());

        return ResponseEntity.noContent().build();
    }
}
