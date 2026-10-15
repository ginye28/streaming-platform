package com.sp.api.live.relay;

import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.live.ingest.IngestProperties;
import com.sp.api.live.access.LiveAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 저지연 시청 연결(/watch?liveId=N).
 *
 *  1. 연결하고 첫 글자 메시지로 {"type":"join","token":"<로그인 토큰, 없어도 됨>"} 을 보낸다.
 *  2. 서버가 {"type":"init","codecs":"avc1.….,mp4a.40.2"} 다음에 머리(이진), 지난 조각(이진), 그 뒤 실시간 조각(이진)을 준다.
 *  3. 방송이 끝나면 {"type":"ended"} 를 주고 닫는다. 방송이 없으면 {"type":"unavailable"}, 저지연 시청자가 가득 차면 {"type":"full"}(HLS 로 본다).
 *
 * 시청자마다 큐와 전송 스레드 하나를 둔다. 느린 시청자가 있어도 중계 스레드(ffmpeg 출력을 읽는 쪽)는 막히지 않고,
 * 큐가 차면 그 시청자만 끊는다(화면은 HLS 로 이어서 본다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiveWatchWebSocketHandler extends AbstractWebSocketHandler {

    /**
     * 큐에 쌓아 둘 최대 조각 수. 100ms 조각이면 약 10초 분량이다. 이만큼 밀리면 망이 못 따라가는 것이라 끊고(HLS 로 이어 본다),
     * 느린 시청자가 서버 메모리를 먹지 못하게 한다(조각 약 19KB × 100 = 2MB).
     */
    private static final int MAX_QUEUE = 100;

    private final LiveRelay relay;
    private final LiveAccessService accessService;
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private final IngestProperties ingestProperties;

    private final Map<String, Viewer> viewers = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        if (viewers.containsKey(session.getId())) {
            return;
        }

        Long liveId = liveIdOf(session.getUri());

        if (liveId == null) {
            close(session, "{\"type\":\"unavailable\"}");
            return;
        }

        String token = "";

        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            token = node.path("token").asString("");
        } catch (Exception ignored) {
            // 토큰 없는 비로그인 시청으로 본다.
        }

        String email = token.isBlank() ? null : jwtProvider.parseEmail(token);

        if (!accessService.canWatchLive(liveId, email)) {
            close(session, "{\"type\":\"forbidden\"}");
            return;
        }

        if (relay.viewerCount(liveId) >= ingestProperties.getLowLatencyMaxViewers()) {
            close(session, "{\"type\":\"full\"}");
            return;
        }

        Viewer viewer = new Viewer(session);

        Runnable leave = relay.join(liveId, viewer);

        if (leave == null) {
            close(session, "{\"type\":\"unavailable\"}");
            return;
        }

        viewer.leave = leave;
        viewers.put(session.getId(), viewer);
        viewer.start();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Viewer viewer = viewers.remove(session.getId());

        if (viewer != null) {
            viewer.stop();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("저지연 시청 연결 오류: {}", exception.getMessage());
        Viewer viewer = viewers.remove(session.getId());

        if (viewer != null) {
            viewer.stop();
        }
    }

    private static Long liveIdOf(URI uri) {

        if (uri == null) {
            return null;
        }

        String value = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("liveId");

        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void close(WebSocketSession session, String lastMessage) {
        try {
            session.sendMessage(new TextMessage(lastMessage));
            session.close(CloseStatus.NORMAL);
        } catch (IOException ignored) {
            // 이미 닫힌 연결
        }
    }

    /** 시청자 한 명. 중계 스레드는 큐에 넣기만 하고, 실제 전송은 이 시청자의 가상 스레드가 한다. */
    private final class Viewer implements LiveRelay.Subscriber {

        private static final Object END = new Object();

        private final WebSocketSession session;
        private final LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        private volatile Runnable leave;
        private volatile boolean stopped;
        private Thread sender;

        Viewer(WebSocketSession session) {
            this.session = session;
        }

        void start() {
            // 가상 스레드가 아니라 일반 스레드를 쓴다. 무료 서버처럼 코어가 1개로 보이는 컨테이너에서는 가상 스레드 실행기도
            // 1개라, 블로킹 전송에 걸린 시청자 하나가 모두를 굶길 수 있다(부하 시험에서 20~40초 멈춤을 확인).
            sender = Thread.ofPlatform().daemon().name("watch-" + session.getId()).start(this::pump);
        }

        void stop() {

            if (stopped) {
                return;
            }

            stopped = true;

            if (leave != null) {
                leave.run();
            }

            queue.clear();

            if (sender != null) {
                sender.interrupt();
            }
        }

        @Override
        public void onInit(String codecs, byte[] init) {
            offer("{\"type\":\"init\",\"codecs\":" + quote(codecs) + "}");
            offer(init);
        }

        @Override
        public void onData(byte[] data) {
            offer(data);
        }

        @Override
        public void onEnd() {
            offer(END);
        }

        private void offer(Object item) {

            if (stopped) {
                return;
            }

            if (queue.size() >= MAX_QUEUE) {
                // 이 시청자만 끊는다. 중계 전체와 다른 시청자는 영향이 없다.
                log.debug("느린 시청자를 끊습니다: {}", session.getId());
                stop();
                closeAsync();
                return;
            }

            queue.add(item);
        }

        private void closeAsync() {
            Thread.ofPlatform().daemon().start(() -> {
                try {
                    session.close(CloseStatus.SERVER_ERROR.withReason("too slow"));
                } catch (IOException ignored) {
                    // 이미 닫힌 연결
                }
            });
        }

        private void pump() {

            try {
                while (!stopped) {

                    Object item = queue.poll(30, TimeUnit.SECONDS);

                    if (item == null) {
                        continue;
                    }

                    if (item == END) {
                        session.sendMessage(new TextMessage("{\"type\":\"ended\"}"));
                        session.close(CloseStatus.NORMAL);
                        return;
                    }

                    if (item instanceof String text) {
                        session.sendMessage(new TextMessage(text));
                    } else {
                        session.sendMessage(new BinaryMessage(ByteBuffer.wrap((byte[]) item)));
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            } catch (Exception e) {
                // 시청자가 나갔거나 전송에 실패했다. 정리만 한다.
                log.debug("저지연 전송 중단: {}", e.getMessage());
                stop();

                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException ignored) {
                    // 이미 닫힌 연결
                }
            }
        }

        private String quote(String value) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                return "\"\"";
            }
        }
    }
}
