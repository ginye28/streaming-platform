package com.sp.api.user.controller;

import com.sp.api.channel.dto.ChannelResponse;
import com.sp.api.channel.service.ChannelService;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.user.dto.ChangePasswordRequest;
import com.sp.api.user.dto.MeResponse;
import com.sp.api.user.dto.StreamKeyResponse;
import com.sp.api.user.dto.UpdateProfileRequest;
import com.sp.api.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 로그인한 본인의 계정 자원. 모든 엔드포인트가 인증을 요구한다.
 * 공개 채널 조회는 /api/channels 에 있다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ChannelService channelService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(authentication.getName())));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                userService.updateProfile(authentication.getName(), request)
        ));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {

        userService.changePassword(authentication.getName(), request);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/me/subscriptions")
    public ResponseEntity<ApiResponse<PageResponse<ChannelResponse>>> mySubscriptions(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                channelService.findMySubscriptions(authentication.getName(), pageable)
        ));
    }

    @GetMapping("/stream-key")
    public ResponseEntity<ApiResponse<StreamKeyResponse>> getStreamKey(Authentication authentication) {

        String streamKey = userService.getStreamKey(authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(new StreamKeyResponse(streamKey)));
    }

    @PostMapping("/stream-key/regenerate")
    public ResponseEntity<ApiResponse<StreamKeyResponse>> regenerateStreamKey(Authentication authentication) {

        String streamKey = userService.regenerateStreamKey(authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(new StreamKeyResponse(streamKey)));
    }
}
