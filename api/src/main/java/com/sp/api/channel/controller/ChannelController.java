package com.sp.api.channel.controller;

import com.sp.api.channel.dto.ChannelResponse;
import com.sp.api.channel.service.ChannelService;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.common.security.AuthUtils;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.service.StreamService;
import com.sp.api.subscribe.dto.SubscribeResponse;
import com.sp.api.subscribe.service.SubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 채널 페이지. 계정 자원(/api/users)과 분리해 조회는 비로그인도 가능하게 한다.
 */
@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;
    private final StreamService streamService;
    private final SubscribeService subscribeService;

    @GetMapping("/{channelId}")
    public ResponseEntity<ApiResponse<ChannelResponse>> findChannel(
            @PathVariable Long channelId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                channelService.findChannel(channelId, AuthUtils.emailOrNull(authentication))
        ));
    }

    @GetMapping("/{channelId}/streams")
    public ResponseEntity<ApiResponse<PageResponse<StreamResponse>>> findChannelStreams(
            @PathVariable Long channelId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                streamService.findByChannel(channelId, pageable, AuthUtils.emailOrNull(authentication))
        ));
    }

    @PostMapping("/{channelId}/subscribe")
    public ResponseEntity<ApiResponse<SubscribeResponse>> toggleSubscribe(
            @PathVariable Long channelId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                subscribeService.toggle(channelId, authentication.getName())
        ));
    }
}
