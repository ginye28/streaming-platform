package com.sp.api.chat.controller;

import com.sp.api.chat.dto.ChatMessageRequest;
import com.sp.api.chat.service.ChatService;
import com.sp.api.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 클라이언트는 /app/lives/{liveId}/chat 으로 보내고
 * /topic/lives/{liveId} 를 구독해 받는다. 방송에 퍼지는 메시지는 ChatService 가 내보낸다.
 *
 * 보내지 못한 이유(제한·슬로우 모드·금칙어 등)는 보낸 사람에게만 /user/queue/chat-errors 로 돌려준다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;

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

        chatService.send(liveId, principal.getName(), request.getContent());
    }

    @MessageExceptionHandler(BusinessException.class)
    @SendToUser("/queue/chat-errors")
    public Map<String, String> handleRejected(BusinessException e) {
        return Map.of("message", e.getMessage());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/chat-errors")
    public Map<String, String> handleInvalid(MethodArgumentNotValidException e) {
        return Map.of("message", "메시지를 확인해 주세요. (1~500자)");
    }
}
