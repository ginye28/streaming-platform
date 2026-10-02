package com.sp.api.chat.controller;

import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.dto.ChatReplayResponse;
import com.sp.api.chat.service.ChatService;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.common.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lives/{liveId}/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 접속 직후 채팅창을 채우기 위한 지난 내역. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChatMessageResponse>>> history(
            @PathVariable Long liveId,
            @PageableDefault(size = 50) Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                chatService.findHistory(liveId, pageable, AuthUtils.emailOrNull(authentication))
        ));
    }

    /** 다시보기 채팅. 방송이 끝난 뒤에만 된다. afterId 다음부터 이어서 받는다. */
    @GetMapping("/replay")
    public ResponseEntity<ApiResponse<ChatReplayResponse>> replay(
            @PathVariable Long liveId,
            @RequestParam(required = false) Long afterId,
            @RequestParam(defaultValue = "200") int size,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                chatService.findReplay(liveId, afterId, size, AuthUtils.emailOrNull(authentication))
        ));
    }
}
