package com.sp.api.integration;

import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.live.ingest.HlsPackager;
import com.sp.api.live.ingest.HlsPackagerFactory;
import com.sp.api.live.relay.FragmentListener;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 저지연 시청(/watch): 방송이 흘려 보낸 조각을 HLS 를 거치지 않고 시청자가 바로 받는지.
 * ffmpeg 는 쓰지 않고, 중계 허브가 건네 준 수신기에 테스트가 직접 머리와 조각을 넣는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:lowlatency;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.ingest.public-base-url=https://api.example.com/",
        "app.ingest.max-sessions=3",
        "app.ingest.low-latency-max-viewers=2"
})
class LowLatencyWatchTest {

    private static final long TIMEOUT_SECONDS = 10;

    /** 방송 이름별로 중계 허브가 건네 준 수신기. */
    static final Map<String, FragmentListener> LISTENERS = new ConcurrentHashMap<>();

    @TestConfiguration
    static class FakePackagerConfig {

        @Bean
        @Primary
        HlsPackagerFactory fakePackagerFactory() {
            return new HlsPackagerFactory() {

                @Override
                public HlsPackager open(String streamName) {
                    return open(streamName, null);
                }

                @Override
                public HlsPackager open(String streamName, FragmentListener listener) {
                    LISTENERS.put(streamName, listener);

                    return new HlsPackager() {
                        @Override
                        public void write(byte[] chunk) {
                        }

                        @Override
                        public void close() {
                        }
                    };
                }
            };
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("시청자는 머리 → 마지막 키프레임부터의 조각 → 실시간 조각 순으로 받고, 방송이 끝나면 끝났다는 알림을 받는다")
    void viewerReceivesInitBacklogThenLive() throws Exception {

        Broadcaster broadcaster = startBroadcast("low1@test.com", "저지연방송1");
        FragmentListener feed = broadcaster.feed;

        feed.onInit("avc1.640028,mp4a.40.2", bytes("INIT"));
        feed.onFragment(bytes("key1"), true);
        feed.onFragment(bytes("inter1"), false);

        Client viewer = connectViewer(broadcaster.liveId);
        viewer.send("{\"type\":\"join\",\"token\":\"\"}");

        JsonNode init = objectMapper.readTree(viewer.nextText());
        assertThat(init.path("type").asString()).isEqualTo("init");
        assertThat(init.path("codecs").asString()).isEqualTo("avc1.640028,mp4a.40.2");

        assertThat(new String(viewer.nextBinary(), StandardCharsets.UTF_8)).isEqualTo("INIT");
        assertThat(new String(viewer.nextBinary(), StandardCharsets.UTF_8)).isEqualTo("key1");
        assertThat(new String(viewer.nextBinary(), StandardCharsets.UTF_8)).isEqualTo("inter1");

        // 합류한 뒤에 오는 조각은 실시간으로 받는다
        feed.onFragment(bytes("inter2"), false);
        assertThat(new String(viewer.nextBinary(), StandardCharsets.UTF_8)).isEqualTo("inter2");

        broadcaster.client.close();

        JsonNode ended = objectMapper.readTree(viewer.nextText());
        assertThat(ended.path("type").asString()).isEqualTo("ended");
    }

    @Test
    @DisplayName("저지연 시청자가 상한을 넘으면 full 을 알려 HLS 로 보게 한다")
    void fullWhenOverCapacity() throws Exception {

        Broadcaster broadcaster = startBroadcast("low2@test.com", "저지연방송2");
        broadcaster.feed.onInit("avc1.640028", bytes("INIT"));

        Client first = connectViewer(broadcaster.liveId);
        first.send("{\"type\":\"join\",\"token\":\"\"}");
        assertThat(objectMapper.readTree(first.nextText()).path("type").asString()).isEqualTo("init");

        Client second = connectViewer(broadcaster.liveId);
        second.send("{\"type\":\"join\",\"token\":\"\"}");
        assertThat(objectMapper.readTree(second.nextText()).path("type").asString()).isEqualTo("init");

        Client third = connectViewer(broadcaster.liveId);
        third.send("{\"type\":\"join\",\"token\":\"\"}");

        assertThat(objectMapper.readTree(third.nextText()).path("type").asString()).isEqualTo("full");

        broadcaster.client.close();
    }

    @Test
    @DisplayName("없는 방송이면 unavailable 을 알려 HLS 로 넘어가게 한다")
    void unavailableForMissingBroadcast() throws Exception {

        Client viewer = connectViewer(987654321L);
        viewer.send("{\"type\":\"join\",\"token\":\"\"}");

        assertThat(objectMapper.readTree(viewer.nextText()).path("type").asString()).isEqualTo("unavailable");
    }

    // ── 도구 ──

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private record Broadcaster(Client client, long liveId, FragmentListener feed) {
    }

    private Broadcaster startBroadcast(String email, String nickname) throws Exception {

        User user = userRepository.save(new User(email, "encoded", nickname));
        String token = jwtProvider.createToken(user.getEmail());

        Client client = connect("/ingest");
        client.send("{\"type\":\"start\",\"token\":\"" + token + "\"}");

        JsonNode started = objectMapper.readTree(client.nextText());
        assertThat(started.path("type").asString()).isEqualTo("started");

        long liveId = started.path("liveId").asLong();

        // 방송 이름으로 허브가 건네 준 수신기를 찾는다
        String body = org.springframework.web.client.RestClient.create("http://localhost:" + port)
                .get().uri("/api/lives/" + liveId).retrieve().body(String.class);
        String hls = objectMapper.readTree(body).path("data").path("hlsUrl").asString();
        String streamName = hls.substring(hls.lastIndexOf('/') + 1, hls.length() - ".m3u8".length());

        FragmentListener feed = LISTENERS.get(streamName);
        assertThat(feed).as("중계 허브 수신기").isNotNull();

        return new Broadcaster(client, liveId, feed);
    }

    private Client connectViewer(long liveId) throws Exception {
        return connect("/watch?liveId=" + liveId);
    }

    private Client connect(String path) throws Exception {

        Client client = new Client();

        client.session = new StandardWebSocketClient()
                .execute(client.handler, new WebSocketHttpHeaders(), URI.create("ws://localhost:" + port + path))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        return client;
    }

    private static final class Client {

        private final BlockingQueue<String> texts = new LinkedBlockingQueue<>();
        private final BlockingQueue<byte[]> binaries = new LinkedBlockingQueue<>();
        private WebSocketSession session;

        private final AbstractWebSocketHandler handler = new AbstractWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                texts.add(message.getPayload());
            }

            @Override
            protected void handleBinaryMessage(WebSocketSession s, BinaryMessage message) {
                byte[] data = new byte[message.getPayloadLength()];
                message.getPayload().get(data);
                binaries.add(data);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
            }
        };

        void send(String text) throws Exception {
            WebSocketMessage<?> message = new TextMessage(text);
            session.sendMessage(message);
        }

        String nextText() throws InterruptedException {
            String text = texts.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(text).as("서버가 보낸 글자 메시지").isNotNull();
            return text;
        }

        byte[] nextBinary() throws InterruptedException {
            byte[] data = binaries.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(data).as("서버가 보낸 이진 메시지").isNotNull();
            return data;
        }

        void close() throws Exception {
            session.close();
        }
    }
}
