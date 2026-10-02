package com.sp.api.chat.event;

import com.sp.api.chat.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 채팅방(WebSocket)으로 내보내는 일을 한 곳에 모은다.
 *
 * 바로 보내지 않고 이벤트로 거치는 이유: DB 에 쓰는 도중에 내보내면, 뒤에서 롤백돼도 시청자 화면에는
 * 이미 나간 뒤라서 화면과 DB 가 어긋난다. 이벤트는 커밋이 끝난 뒤에 나간다.
 */
@Component
@RequiredArgsConstructor
public class ChatEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void message(Long liveId, ChatMessageResponse message) {
        publisher.publishEvent(new ChatBroadcast("/topic/lives/" + liveId, message));
    }

    public void event(Long liveId, ChatEvent event) {
        publisher.publishEvent(new ChatBroadcast("/topic/lives/" + liveId + "/events", event));
    }
}
