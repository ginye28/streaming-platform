package com.sp.api.live.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.dto.LiveSettingRequest;
import com.sp.api.live.dto.LiveSettingResponse;
import com.sp.api.live.dto.LiveStreamResponse;
import com.sp.api.live.service.LiveStreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lives")
@RequiredArgsConstructor
public class LiveController {

    private final LiveStreamService liveStreamService;

    /** 지금 방송 중인 목록. 치지직·유튜브의 라이브 탭에 해당한다. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<LiveStreamResponse>>> liveNow(
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return ResponseEntity.ok(ApiResponse.ok(liveStreamService.findLiveNow(pageable)));
    }

    /** 다음 방송에 쓸 제목·설명·썸네일. 리터럴 경로라 /{liveId} 보다 먼저 선언한다. */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<LiveSettingResponse>> getSetting(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                liveStreamService.getSetting(authentication.getName())
        ));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<LiveSettingResponse>> updateSetting(
            @Valid @RequestBody LiveSettingRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                liveStreamService.updateSetting(authentication.getName(), request)
        ));
    }

    @GetMapping("/{liveId}")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> findById(@PathVariable Long liveId) {
        return ResponseEntity.ok(ApiResponse.ok(liveStreamService.findById(liveId)));
    }
}
