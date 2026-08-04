package com.sp.api.subscribe.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.subscribe.dto.SubscribeResponse;
import com.sp.api.subscribe.service.SubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

        boolean subscribed = subscribeService.toggle(
                channelId,
                authentication.getName()
        );

        long subscriberCount = subscribeService.count(channelId);

        SubscribeResponse response = new SubscribeResponse(
                subscribed,
                subscriberCount
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }
}