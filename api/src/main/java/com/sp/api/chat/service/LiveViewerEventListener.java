package com.sp.api.chat.service;

import com.sp.api.chat.dto.ViewerCountMessage;
import com.sp.api.live.service.LiveViewerTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 채팅방 구독/해제를 시청자 수로 환산해 방송별로 브로드캐스트한다.
 */
@Component
@RequiredArgsConstructor
public class LiveViewerEventListener {

    private static final Pattern LIVE_TOPIC = Pattern.compile("^/topic/lives/(\\d+)$");

    private final LiveViewerTracker viewerTracker;
    private final SimpMessagingTemplate messagingTemplate;

    /** 세션이 끊길 때 어느 방송을 보고 있었는지 알아야 하므로 기억해 둔다. */
    private final Map<String, Long> liveIdBySession = new ConcurrentHashMap<>();

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        Long liveId = parseLiveId(accessor.getDestination());
        String sessionId = accessor.getSessionId();

        if (liveId == null || sessionId == null) {
            return;
        }

        liveIdBySession.put(sessionId, liveId);

        broadcast(liveId, viewerTracker.join(liveId, sessionId));
    }

    @EventListener
    public void onUnsubscribe(SessionUnsubscribeEvent event) {
        leave(StompHeaderAccessor.wrap(event.getMessage()).getSessionId());
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        leave(event.getSessionId());
    }

    private void leave(String sessionId) {

        if (sessionId == null) {
            return;
        }

        Long liveId = liveIdBySession.remove(sessionId);

        if (liveId != null) {
            broadcast(liveId, viewerTracker.leave(liveId, sessionId));
        }
    }

    private void broadcast(Long liveId, long viewerCount) {
        messagingTemplate.convertAndSend(
                "/topic/lives/" + liveId + "/viewers",
                new ViewerCountMessage(liveId, viewerCount)
        );
    }

    private Long parseLiveId(String destination) {

        if (destination == null) {
            return null;
        }

        Matcher matcher = LIVE_TOPIC.matcher(destination);

        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }
}
