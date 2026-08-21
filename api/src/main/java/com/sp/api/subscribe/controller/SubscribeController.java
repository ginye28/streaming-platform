package com.sp.api.subscribe.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.subscribe.dto.SubscribeResponse;
import com.sp.api.subscribe.service.SubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{channelId}/subscribe")
@RequiredArgsConstructor
public class SubscribeController {

    private final SubscribeService subscribeService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscribeResponse>> toggle(
            @PathVariable Long channelId,
            Authentication authentication
    ) {

        SubscribeResponse response =
                subscribeService.toggle(channelId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
