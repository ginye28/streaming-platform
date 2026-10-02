package com.sp.api.chat.controller;

import com.sp.api.chat.dto.BannedWordRequest;
import com.sp.api.chat.dto.BannedWordResponse;
import com.sp.api.chat.dto.ModeratorResponse;
import com.sp.api.chat.dto.RestrictionResponse;
import com.sp.api.chat.moderation.ChatModerationService;
import com.sp.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채널 주인이 내 채널의 채팅 도구를 관리한다 — 매니저, 금칙어, 채팅이 막힌 사람.
 * 모두 "내 채널" 이라 /api/users/me 아래에 두었고, 그래서 로그인한 본인만 읽고 쓴다.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class ChatToolsController {

    private final ChatModerationService moderationService;

    @GetMapping("/moderators")
    public ResponseEntity<ApiResponse<List<ModeratorResponse>>> moderators(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.moderators(authentication.getName())));
    }

    @PostMapping("/moderators/{userId}")
    public ResponseEntity<ApiResponse<ModeratorResponse>> addModerator(
            @PathVariable Long userId, Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                moderationService.addModerator(authentication.getName(), userId)));
    }

    @DeleteMapping("/moderators/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeModerator(
            @PathVariable Long userId, Authentication authentication) {

        moderationService.removeModerator(authentication.getName(), userId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/banned-words")
    public ResponseEntity<ApiResponse<List<BannedWordResponse>>> bannedWords(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.bannedWords(authentication.getName())));
    }

    @PostMapping("/banned-words")
    public ResponseEntity<ApiResponse<BannedWordResponse>> addBannedWord(
            @Valid @RequestBody BannedWordRequest request, Authentication authentication) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                moderationService.addBannedWord(authentication.getName(), request.word())));
    }

    @DeleteMapping("/banned-words/{wordId}")
    public ResponseEntity<ApiResponse<Void>> removeBannedWord(
            @PathVariable Long wordId, Authentication authentication) {

        moderationService.removeBannedWord(authentication.getName(), wordId);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/chat-restrictions")
    public ResponseEntity<ApiResponse<List<RestrictionResponse>>> restrictions(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(moderationService.restrictions(authentication.getName())));
    }

    @DeleteMapping("/chat-restrictions/{userId}")
    public ResponseEntity<ApiResponse<Void>> lift(@PathVariable Long userId, Authentication authentication) {

        moderationService.liftByOwner(authentication.getName(), userId);

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
