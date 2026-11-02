package com.sp.api.live.controller;

import com.sp.api.live.dto.PlaybackReportRequest;
import com.sp.api.live.relay.PlaybackStats;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PostMapping("/report")
    public ResponseEntity<Void> report(@Valid @RequestBody PlaybackReportRequest request) {

        stats.report(request.mode(), request.reason());

        return ResponseEntity.noContent().build();
    }
}
