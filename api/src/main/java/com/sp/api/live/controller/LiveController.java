package com.sp.api.live.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.common.security.AuthUtils;
import com.sp.api.common.web.ViewerKey;
import com.sp.api.live.dto.LiveSettingRequest;
import com.sp.api.live.dto.LiveSettingResponse;
import com.sp.api.live.dto.LiveStreamResponse;
import com.sp.api.live.service.LiveStreamService;
import jakarta.servlet.http.HttpServletRequest;
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
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                liveStreamService.findLiveNow(pageable, AuthUtils.emailOrNull(authentication))
        ));
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

    /**
     * "이어보기" — 아직 안 본 방송 하나. 인트로에서 [다음 방송] 을 눌렀을 때 쓴다.
     * 리터럴 경로라 /{liveId} 보다 먼저 선언한다.
     */
    @GetMapping("/next")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> next(
            @RequestParam(required = false) Long excludeLiveId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        String email = AuthUtils.emailOrNull(authentication);

        return ResponseEntity.ok(ApiResponse.ok(
                liveStreamService.findNext(email, ViewerKey.of(email, request), excludeLiveId)
        ));
    }

    @GetMapping("/{liveId}")
    public ResponseEntity<ApiResponse<LiveStreamResponse>> findById(@PathVariable Long liveId) {
        return ResponseEntity.ok(ApiResponse.ok(liveStreamService.findById(liveId)));
    }
}
