package com.sp.api.chat.controller;

import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.service.ChatService;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
            @PageableDefault(size = 50) Pageable pageable
    ) {

        return ResponseEntity.ok(ApiResponse.ok(chatService.findHistory(liveId, pageable)));
    }
}
