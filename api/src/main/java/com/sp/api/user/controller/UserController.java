package com.sp.api.user.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.user.dto.StreamKeyResponse;
import com.sp.api.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public String me(Authentication authentication) {
        return authentication.getName();
    }

    @GetMapping("/stream-key")
    public ResponseEntity<ApiResponse<StreamKeyResponse>> getStreamKey(
            Authentication authentication
    ) {

        String streamKey = userService.getStreamKey(authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(true, new StreamKeyResponse(streamKey))
        );
    }
}
