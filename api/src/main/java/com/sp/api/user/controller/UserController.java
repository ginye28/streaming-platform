package com.sp.api.user.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.user.dto.MeResponse;
import com.sp.api.user.dto.StreamKeyResponse;
import com.sp.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MeResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMe(authentication.getName())));
    }

    @GetMapping("/stream-key")
    public ResponseEntity<ApiResponse<StreamKeyResponse>> getStreamKey(
            Authentication authentication
    ) {

        String streamKey = userService.getStreamKey(authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(new StreamKeyResponse(streamKey)));
    }

    @PostMapping("/stream-key/regenerate")
    public ResponseEntity<ApiResponse<StreamKeyResponse>> regenerateStreamKey(
            Authentication authentication
    ) {

        String streamKey = userService.regenerateStreamKey(authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(new StreamKeyResponse(streamKey)));
    }
}
