package com.sp.api.vtuber.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.vtuber.dto.ChannelProfileRequest;
import com.sp.api.vtuber.dto.ChannelProfileResponse;
import com.sp.api.vtuber.service.ChannelProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChannelProfileController {

    private final ChannelProfileService channelProfileService;

    /** 채널 페이지에서 보여 줄 그 사람의 정보. 아직 아무것도 안 채운 채널도 같은 형태로 온다. */
    @GetMapping("/api/channels/{channelId}/profile")
    public ResponseEntity<ApiResponse<ChannelProfileResponse>> find(@PathVariable Long channelId) {
        return ResponseEntity.ok(ApiResponse.ok(channelProfileService.find(channelId)));
    }

    @GetMapping("/api/users/me/channel-profile")
    public ResponseEntity<ApiResponse<ChannelProfileResponse>> findMine(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                channelProfileService.findMine(authentication.getName())
        ));
    }

    @PutMapping("/api/users/me/channel-profile")
    public ResponseEntity<ApiResponse<ChannelProfileResponse>> updateMine(
            @Valid @RequestBody ChannelProfileRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                channelProfileService.updateMine(authentication.getName(), request)
        ));
    }
}
