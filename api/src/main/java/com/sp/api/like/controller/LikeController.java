package com.sp.api.like.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.like.dto.LikeResponse;
import com.sp.api.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streams/{streamId}/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ResponseEntity<ApiResponse<LikeResponse>> toggle(@PathVariable long streamId, Authentication authentication) {

        boolean liked = likeService.toggle(streamId, authentication.getName());
        long count = likeService.count(streamId);

        LikeResponse response = new LikeResponse(liked, count);

        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }
}
