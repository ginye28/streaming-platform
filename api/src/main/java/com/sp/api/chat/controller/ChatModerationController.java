package com.sp.api.chat.controller;

import com.sp.api.chat.dto.ChatAudienceRequest;
import com.sp.api.chat.dto.PinRequest;
import com.sp.api.chat.dto.RestrictRequest;
import com.sp.api.chat.dto.RestrictionResponse;
import com.sp.api.chat.dto.SlowModeRequest;
import com.sp.api.chat.moderation.ChatModerationService;
import com.sp.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 방송 안에서 채널 주인·매니저가 하는 채팅 운영. 로그인이 필요하고, 권한은 서비스가 방송의 채널로 따진다.
 * (GET 이 아니라서 SecurityConfig 의 공개 규칙에 걸리지 않는다.)
 */
@RestController
@RequestMapping("/api/lives/{liveId}")
@RequiredArgsConstructor
public class ChatModerationController {

    private final ChatModerationService moderationService;

    @DeleteMapping("/chats/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(
            @PathVariable Long liveId, @PathVariable Long messageId, Authentication authentication) {

        moderationService.deleteMessage(liveId, messageId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/pin")
    public ResponseEntity<ApiResponse<Void>> pin(
            @PathVariable Long liveId, @Valid @RequestBody PinRequest request, Authentication authentication) {

        moderationService.pin(liveId, request.messageId(), authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/pin")
    public ResponseEntity<ApiResponse<Void>> unpin(@PathVariable Long liveId, Authentication authentication) {

        moderationService.unpin(liveId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/slow-mode")
    public ResponseEntity<ApiResponse<Void>> slowMode(
            @PathVariable Long liveId, @Valid @RequestBody SlowModeRequest request, Authentication authentication) {

        moderationService.setSlowMode(liveId, request.seconds(), authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/chat-audience")
    public ResponseEntity<ApiResponse<Void>> chatAudience(
            @PathVariable Long liveId, @Valid @RequestBody ChatAudienceRequest request, Authentication authentication) {

        moderationService.setChatAudience(liveId, request.chatAudience(), authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /** 일시 정지(minutes 가 있음) 또는 강퇴(없음). */
    @PostMapping("/restrictions")
    public ResponseEntity<ApiResponse<RestrictionResponse>> restrict(
            @PathVariable Long liveId, @Valid @RequestBody RestrictRequest request, Authentication authentication) {

        return ResponseEntity.ok(ApiResponse.ok(
                moderationService.restrict(liveId, request, authentication.getName())));
    }

    @DeleteMapping("/restrictions/{userId}")
    public ResponseEntity<ApiResponse<Void>> lift(
            @PathVariable Long liveId, @PathVariable Long userId, Authentication authentication) {

        moderationService.lift(liveId, userId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
