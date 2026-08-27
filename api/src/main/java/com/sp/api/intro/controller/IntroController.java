package com.sp.api.intro.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.security.AuthUtils;
import com.sp.api.common.web.ViewerKey;
import com.sp.api.intro.dto.IntroRequest;
import com.sp.api.intro.dto.IntroResponse;
import com.sp.api.intro.dto.IntroSeenRequest;
import com.sp.api.intro.service.IntroService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class IntroController {

    private final IntroService introService;

    /** 방송에 처음 들어온 사람에게 보여 줄 자기소개. 띄울지 말지는 응답의 showGate 가 정한다. */
    @GetMapping("/api/channels/{channelId}/intro")
    public ResponseEntity<ApiResponse<IntroResponse>> findIntro(
            @PathVariable Long channelId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        String email = AuthUtils.emailOrNull(authentication);

        return ResponseEntity.ok(ApiResponse.ok(
                introService.findForViewer(channelId, ViewerKey.of(email, request), email)
        ));
    }

    /** 방송 화면이 쓰는 통로. 방송 정보와 나란히 받아야 인트로가 늦게 떠서 방송이 새지 않는다. */
    @GetMapping("/api/lives/{liveId}/intro")
    public ResponseEntity<ApiResponse<IntroResponse>> findIntroForLive(
            @PathVariable Long liveId,
            Authentication authentication,
            HttpServletRequest request
    ) {

        String email = AuthUtils.emailOrNull(authentication);

        return ResponseEntity.ok(ApiResponse.ok(
                introService.findForLive(liveId, ViewerKey.of(email, request), email)
        ));
    }

    /** 인트로를 보고 무엇을 했는지 남긴다. 로그인하지 않아도 접속 IP 로 기억한다. */
    @PostMapping("/api/channels/{channelId}/intro/seen")
    public ResponseEntity<ApiResponse<Void>> recordSeen(
            @PathVariable Long channelId,
            @Valid @RequestBody IntroSeenRequest body,
            Authentication authentication,
            HttpServletRequest request
    ) {

        String email = AuthUtils.emailOrNull(authentication);

        introService.recordSeen(channelId, ViewerKey.of(email, request), body.getAction());

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/api/users/me/intro")
    public ResponseEntity<ApiResponse<IntroResponse>> findMine(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(introService.findMine(authentication.getName())));
    }

    @PutMapping("/api/users/me/intro")
    public ResponseEntity<ApiResponse<IntroResponse>> updateMine(
            @Valid @RequestBody IntroRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                introService.updateMine(authentication.getName(), request)
        ));
    }
}
