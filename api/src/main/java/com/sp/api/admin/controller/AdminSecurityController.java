package com.sp.api.admin.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.security.SecurityEventRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 비정상 접근 기록 조회. 인가는 SecurityConfig 에서 ADMIN 으로 제한한다(/api/admin/**).
 * 서버를 재시작하면 비워지는 메모리 기록이다(SecurityEventRecorder).
 */
@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final SecurityEventRecorder recorder;

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<SecurityEventRecorder.Snapshot>> events() {
        return ResponseEntity.ok(ApiResponse.ok(recorder.snapshot()));
    }
}
