package com.sp.api.integration;

import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.live.ingest.HlsPackager;
import com.sp.api.live.ingest.HlsPackagerFactory;
import com.sp.api.live.ingest.IngestProperties;
import com.sp.api.live.service.LiveStreamService;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 브라우저에서 바로 방송하는 연결(/ingest)을 실제 WebSocket 으로 확인한다.
 * ffmpeg 는 쓰지 않고, 받은 바이트를 모으는 가짜 포장기로 바꿔 끼운다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:browseringest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.ingest.public-base-url=https://api.example.com/",
        "app.ingest.max-sessions=2"
})
class BrowserIngestTest {

    private static final long TIMEOUT_SECONDS = 10;

    /** 방송 이름별로 받은 바이트와 닫혔는지를 모아 둔다. */
    static final Map<String, FakePackager> PACKAGERS = new ConcurrentHashMap<>();

    @TestConfiguration
    static class FakePackagerConfig {

        @Bean
        @Primary
        HlsPackagerFactory fakePackagerFactory() {
            return streamName -> {
                FakePackager packager = new FakePackager();
                PACKAGERS.put(streamName, packager);
                return packager;
            };
        }
    }

    static class FakePackager implements HlsPackager {

        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        volatile boolean closed;

        @Override
        public synchronized void write(byte[] chunk) {
            received.write(chunk, 0, chunk.length);
        }

        @Override
        public void close() {
            closed = true;
        }

        synchronized int size() {
            return received.size();
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

    @Autowired
    private LiveStreamService liveStreamService;

    @Autowired
    private IngestProperties ingestProperties;

    @Test
    @DisplayName("로그인한 사용자가 연결해 조각을 보내면 방송이 열리고, 끊으면 다시보기 없이 끝난다")
    void browserBroadcastLifecycle() throws Exception {

        String token = tokenOf("ing1@test.com", "브라우저방송");

        Client client = connect();
        client.send("{\"type\":\"start\",\"token\":\"" + token + "\"}");

        JsonNode started = objectMapper.readTree(client.next());
        assertThat(started.path("type").asString()).isEqualTo("started");
        long liveId = started.path("liveId").asLong();

        // 방송 목록에 뜨고, 재생 주소는 API 자신의 /live-hls 아래다 (스트리밍 서버가 아니라)
        JsonNode live = getJson("/api/lives/" + liveId);
        String hlsUrl = live.path("data").path("hlsUrl").asString();

        assertThat(live.path("data").path("status").asString()).isEqualTo("LIVE");
        assertThat(hlsUrl).startsWith("https://api.example.com/live-hls/b-").endsWith(".m3u8");

        String streamName = hlsUrl.substring(hlsUrl.lastIndexOf('/') + 1, hlsUrl.length() - ".m3u8".length());

        client.sendBinary(new byte[]{1, 2, 3, 4});
        client.sendBinary(new byte[]{5, 6});

        FakePackager packager = PACKAGERS.get(streamName);
        awaitTrue(() -> packager.size() == 6);

        client.close();

        awaitTrue(() -> packager.closed);

        awaitTrue(() -> "ENDED".equals(getJson("/api/lives/" + liveId).path("data").path("status").asString()));

        JsonNode ended = getJson("/api/lives/" + liveId);
        assertThat(ended.path("data").path("vodUrl").isNull()).isTrue();
    }

    @Test
    @DisplayName("로그인 토큰이 틀리면 방송이 열리지 않는다")
    void invalidTokenIsRejected() throws Exception {

        Client client = connect();
        client.send("{\"type\":\"start\",\"token\":\"not-a-token\"}");

        JsonNode error = objectMapper.readTree(client.next());

        assertThat(error.path("type").asString()).isEqualTo("error");
        assertThat(error.path("message").asString()).contains("로그인");

        assertThat(client.awaitClosed()).isTrue();
    }

    @Test
    @DisplayName("시작 메시지 없이 영상부터 보내면 끊긴다")
    void binaryBeforeStartIsRejected() throws Exception {

        Client client = connect();
        client.sendBinary(new byte[]{1, 2, 3});

        assertThat(client.awaitClosed()).isTrue();
    }

    @Test
    @DisplayName("같은 계정이 동시에 두 곳에서 방송하려 하면 두 번째는 거절한다")
    void sameUserCannotBroadcastTwice() throws Exception {

        String token = tokenOf("ing-dup@test.com", "중복방송");

        Client first = connect();
        first.send("{\"type\":\"start\",\"token\":\"" + token + "\"}");
        assertThat(objectMapper.readTree(first.next()).path("type").asString()).isEqualTo("started");

        Client second = connect();
        second.send("{\"type\":\"start\",\"token\":\"" + token + "\"}");

        JsonNode error = objectMapper.readTree(second.next());
        assertThat(error.path("type").asString()).isEqualTo("error");
        assertThat(error.path("message").asString()).contains("이미");

        first.close();
    }

    @Test
    @DisplayName("너무 빠르게 보내면 방송을 끊는다")
    void tooFastIsCutOff() throws Exception {

        String token = tokenOf("ing-fast@test.com", "과속");

        Client client = connect();
        client.send("{\"type\":\"start\",\"token\":\"" + token + "\"}");
        client.next();

        // 허용량은 10초 창에서 6000kbps(750KB/s)의 두 배 = 15MB. 그것을 넘는 20MB 를 한꺼번에
        byte[] big = new byte[2 * 1024 * 1024];
        for (int i = 0; i < 10; i++) {
            client.sendBinary(big);
        }

        assertThat(client.awaitClosed()).isTrue();
    }

    @Test
    @DisplayName("서버가 다시 켜질 때 끊긴 브라우저 방송은 정리된다")
    void staleBrowserBroadcastIsClosedOnRestart() {

        User user = userRepository.save(new User("ing-stale@test.com", "encoded", "남은방송"));

        var live = liveStreamService.startBrowserBroadcast(user.getStreamKey());

        assertThat(live.getStreamName()).startsWith("b-");
        assertThat(liveStreamService.closeStaleBrowserBroadcasts()).isGreaterThanOrEqualTo(1);
        assertThat(liveStreamService.findById(live.getId(), null).status()).isEqualTo("ENDED");
    }

    @Test
    @DisplayName("만들어 둔 HLS 파일은 로그인 없이 내려 받을 수 있다")
    void hlsFilesAreServed() throws Exception {

        Path dir = ingestProperties.getHlsDir();
        Files.createDirectories(dir);
        Path playlist = dir.resolve("b-served-test.m3u8");
        Files.writeString(playlist, "#EXTM3U\n", StandardCharsets.UTF_8);

        try {
            String body = RestClient.create("http://localhost:" + port)
                    .get().uri("/live-hls/b-served-test.m3u8")
                    .retrieve().body(String.class);

            assertThat(body).startsWith("#EXTM3U");
        } finally {
            Files.deleteIfExists(playlist);
        }
    }

    // ── 도우미 ──

    private String tokenOf(String email, String nickname) {
        User user = userRepository.save(new User(email, "encoded", nickname));
        return jwtProvider.createToken(user.getEmail());
    }

    private JsonNode getJson(String path) {
        String body = RestClient.create("http://localhost:" + port)
                .get().uri(path).retrieve().body(String.class);
        return objectMapper.readTree(body);
    }

    private Client connect() throws Exception {

        Client client = new Client();

        WebSocketSession session = new StandardWebSocketClient()
                .execute(client.handler, new WebSocketHttpHeaders(),
                        URI.create("ws://localhost:" + port + "/ingest"))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        client.session = session;

        return client;
    }

    private static void awaitTrue(java.util.function.BooleanSupplier condition) throws InterruptedException {

        long deadline = System.currentTimeMillis() + TIMEOUT_SECONDS * 1000;

        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }

        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static final class Client {

        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final java.util.concurrent.CountDownLatch closed = new java.util.concurrent.CountDownLatch(1);
        private WebSocketSession session;

        private final TextWebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                messages.add(message.getPayload());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
                closed.countDown();
            }
        };

        void send(String text) throws Exception {
            session.sendMessage(new TextMessage(text));
        }

        void sendBinary(byte[] data) {
            try {
                session.sendMessage(new BinaryMessage(data));
            } catch (Exception e) {
                // 서버가 이미 끊었을 수 있다. 끊겼는지는 awaitClosed 로 확인한다.
            }
        }

        String next() throws InterruptedException {
            String message = messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(message).as("서버가 보낸 메시지").isNotNull();
            return message;
        }

        boolean awaitClosed() throws InterruptedException {
            return closed.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        void close() throws Exception {
            session.close();
        }
    }
}
