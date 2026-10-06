package com.sp.api.live.ingest;

import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.service.LiveStreamService;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 브라우저에서 바로 방송하는 연결(/ingest)을 받는다.
 *
 *  1. 연결하고 첫 글자 메시지로 {"type":"start","token":"<로그인 토큰>"} 을 보낸다. (WebSocket 은 헤더를 못 붙이므로 메시지로 인증한다)
 *  2. 서버가 방송을 열고 {"type":"started","liveId":N} 을 돌려준다.
 *  3. 그 뒤로 MediaRecorder 가 만든 WebM 조각(이진 메시지)을 계속 보낸다. 서버는 ffmpeg 로 HLS 로 만든다.
 *  4. 연결이 끊기면 방송이 끝난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestWebSocketHandler extends AbstractWebSocketHandler {

    /** 보내는 속도를 재는 창(초). */
    private static final long RATE_WINDOW_MILLIS = 10_000;

    /** MediaRecorder 한 조각의 최대 크기. 키프레임이 든 조각은 커서 넉넉히 잡는다. */
    private static final int MAX_CHUNK_BYTES = 4 * 1024 * 1024;

    private final IngestProperties properties;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final LiveStreamService liveStreamService;
    private final HlsPackagerFactory packagerFactory;
    private final ObjectMapper objectMapper;

    private final Map<String, Broadcast> broadcasts = new ConcurrentHashMap<>();
    private final Set<Long> activeUsers = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 기본 한도(수 KB)로는 영상 조각이 들어오지 못한다. 연결마다 크기를 정해 준다.
        session.setBinaryMessageSizeLimit(MAX_CHUNK_BYTES);
        session.setTextMessageSizeLimit(16 * 1024);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        if (broadcasts.containsKey(session.getId())) {
            return;
        }

        JsonNode node;

        try {
            node = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            fail(session, "잘못된 메시지입니다.");
            return;
        }

        if (!"start".equals(node.path("type").asString(""))) {
            fail(session, "먼저 방송 시작 메시지를 보내야 합니다.");
            return;
        }

        start(session, node.path("token").asString(""));
    }

    private void start(WebSocketSession session, String token) throws IOException {

        if (!properties.isEnabled()) {
            fail(session, "이 서버는 브라우저 방송을 받지 않습니다.");
            return;
        }

        String email = token.isBlank() ? null : jwtProvider.parseEmail(token);
        User user = email == null ? null : userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            fail(session, "로그인이 필요합니다.");
            return;
        }

        if (broadcasts.size() >= properties.getMaxSessions()) {
            fail(session, "지금은 동시에 방송할 수 있는 수가 가득 찼습니다. 잠시 뒤에 다시 시도해 주세요.");
            return;
        }

        if (!activeUsers.add(user.getId())) {
            fail(session, "이 계정은 이미 브라우저로 방송 중입니다.");
            return;
        }

        LiveStream live;
        HlsPackager packager;

        try {
            live = liveStreamService.startBrowserBroadcast(user.getStreamKey());
            packager = packagerFactory.open(live.getStreamName());
        } catch (Exception e) {
            activeUsers.remove(user.getId());
            log.warn("브라우저 방송을 열지 못했습니다: {}", e.getMessage());
            fail(session, "방송을 시작하지 못했습니다.");
            return;
        }

        broadcasts.put(session.getId(), new Broadcast(user.getId(), live.getStreamName(), packager));

        session.sendMessage(new TextMessage(
                "{\"type\":\"started\",\"liveId\":" + live.getId() + "}"));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {

        Broadcast broadcast = broadcasts.get(session.getId());

        if (broadcast == null) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        byte[] chunk = new byte[message.getPayloadLength()];
        message.getPayload().get(chunk);

        if (broadcast.exceedsRate(chunk.length, properties.getMaxKbps())) {
            log.warn("브라우저 방송이 허용 속도를 넘어 끊습니다. {}", broadcast.streamName);
            closeQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("too fast"));
            return;
        }

        try {
            broadcast.packager.write(chunk);
        } catch (IOException e) {
            log.warn("브라우저 방송 포장이 멈췄습니다: {}", e.getMessage());
            closeQuietly(session, CloseStatus.SERVER_ERROR.withReason("packager stopped"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Broadcast broadcast = broadcasts.remove(session.getId());

        if (broadcast == null) {
            return;
        }

        broadcast.packager.close();
        activeUsers.remove(broadcast.userId);

        try {
            liveStreamService.endBrowserBroadcast(broadcast.streamName);
        } catch (Exception e) {
            log.warn("브라우저 방송을 닫지 못했습니다: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("브라우저 방송 연결 오류: {}", exception.getMessage());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    private void fail(WebSocketSession session, String message) throws IOException {
        session.sendMessage(new TextMessage(
                "{\"type\":\"error\",\"message\":" + objectMapper.writeValueAsString(message) + "}"));
        closeQuietly(session, CloseStatus.POLICY_VIOLATION);
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
            // 이미 닫힌 연결
        }
    }

    /** 방송 하나의 상태. 보낸 양을 재서 허용 속도를 넘으면 끊는다. */
    private static final class Broadcast {

        private final Long userId;
        private final String streamName;
        private final HlsPackager packager;

        private long windowStart = System.currentTimeMillis();
        private long windowBytes;

        Broadcast(Long userId, String streamName, HlsPackager packager) {
            this.userId = userId;
            this.streamName = streamName;
            this.packager = packager;
        }

        /** 키프레임 때 순간적으로 커지므로 창(10초) 평균으로 잰다. */
        boolean exceedsRate(int bytes, int maxKbps) {

            long now = System.currentTimeMillis();

            if (now - windowStart >= RATE_WINDOW_MILLIS) {
                windowStart = now;
                windowBytes = 0;
            }

            windowBytes += bytes;

            long allowed = (long) maxKbps * 1000 / 8 * (RATE_WINDOW_MILLIS / 1000) * 2;

            return windowBytes > allowed;
        }
    }
}
