package com.sp.api.like.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.like.dto.LikeResponse;
import com.sp.api.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/streams/{streamId}/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponse>> toggle(
            @PathVariable Long streamId,
            Authentication authentication
    ) {

        LikeResponse response = likeService.toggle(streamId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
