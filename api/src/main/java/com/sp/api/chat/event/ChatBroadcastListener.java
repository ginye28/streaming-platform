package com.sp.api.chat.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 커밋이 끝난 뒤에 메시지를 내보낸다. 트랜잭션이 없으면 바로 내보낸다. */
@Component
@RequiredArgsConstructor
public class ChatBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ChatBroadcast broadcast) {
        messagingTemplate.convertAndSend(broadcast.destination(), broadcast.payload());
    }
}
