package com.sp.api.chat.controller;

import com.sp.api.chat.dto.ChatMessageRequest;
import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 클라이언트는 /app/lives/{liveId}/chat 으로 보내고
 * /topic/lives/{liveId} 를 구독해 받는다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/lives/{liveId}/chat")
    public void handleChat(
            @DestinationVariable Long liveId,
            @Valid ChatMessageRequest request,
            Principal principal
    ) {

        if (principal == null) {
            // 비로그인 연결은 읽기 전용이다.
            log.debug("인증되지 않은 채팅 전송 시도");
            return;
        }

        ChatMessageResponse response =
                chatService.send(liveId, principal.getName(), request.getContent());

        messagingTemplate.convertAndSend("/topic/lives/" + liveId, response);
    }
}
