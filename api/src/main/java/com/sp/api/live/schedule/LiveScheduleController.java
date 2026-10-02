package com.sp.api.live.schedule;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.common.security.AuthUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 방송 예약. 목록과 하나 조회는 누구나, 만들기·고치기·취소와 내 예약은 로그인이 필요하다.
 * /api/lives/{liveId} 보다 먼저 맞도록 리터럴 경로(schedules)를 쓴다.
 */
@RestController
@RequestMapping("/api/lives/schedules")
@RequiredArgsConstructor
public class LiveScheduleController {

    private final LiveScheduleService scheduleService;

    /** 앞으로의 방송 예약, 가까운 순. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LiveScheduleResponse>>> upcoming(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.findUpcoming(pageable, AuthUtils.emailOrNull(authentication))));
    }

    /** 내 예약. 공개 규칙(GET /api/lives/**)보다 먼저 로그인이 필요하다고 SecurityConfig 에 적어 둔다. */
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<LiveScheduleResponse>>> mine(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleService.findMine(authentication.getName())));
    }

    @GetMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<LiveScheduleResponse>> findById(
            @PathVariable Long scheduleId, Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.findById(scheduleId, AuthUtils.emailOrNull(authentication))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LiveScheduleResponse>> create(
            @Valid @RequestBody LiveScheduleRequest request, Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                scheduleService.create(authentication.getName(), request)));
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<LiveScheduleResponse>> update(
            @PathVariable Long scheduleId,
            @Valid @RequestBody LiveScheduleRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                scheduleService.update(authentication.getName(), scheduleId, request)));
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long scheduleId, Authentication authentication) {

        scheduleService.cancel(authentication.getName(), scheduleId);

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
