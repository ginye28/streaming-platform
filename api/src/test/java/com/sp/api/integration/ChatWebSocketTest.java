package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 WebSocket 연결로 채팅이 오가는지 확인한다.
 * 다른 통합 테스트와 스프링 컨텍스트가 다르므로 DB 도 따로 쓴다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:chatws;MODE=MySQL;DB_CLOSE_DELAY=-1")
class ChatWebSocketTest {

    private static final long TIMEOUT_SECONDS = 10;

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.sp.api.user.repository.UserRepository userRepository;

    @Autowired
    private com.sp.api.live.service.LiveStreamService liveStreamService;

    @Autowired
    private com.sp.api.common.jwt.JwtProvider jwtProvider;

    private RestClient restClient;

    @org.junit.jupiter.api.BeforeEach
    void setUpClient() {
        restClient = RestClient.create("http://localhost:" + port);
    }

    @Test
    @DisplayName("구독자가 보낸 채팅이 같은 방송을 보는 사람에게 전달된다")
    void chatIsBroadcastToRoom() throws Exception {

        String token = signupAndLogin("chat@test.com", "채팅유저");
        long liveId = startBroadcastAndGetLiveId(token);

        StompSession session = connect(token);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        subscribe(session, "/topic/lives/" + liveId, received);

        send(session, "/app/lives/" + liveId + "/chat", "{\"content\":\"안녕하세요\"}");

        JsonNode message = objectMapper.readTree(poll(received));

        assertThat(message.path("content").asString()).isEqualTo("안녕하세요");
        assertThat(message.path("nickname").asString()).isEqualTo("채팅유저");
        assertThat(message.path("liveId").asLong()).isEqualTo(liveId);

        session.disconnect();
    }

    @Test
    @DisplayName("보낸 채팅은 내역 조회 API 로도 다시 볼 수 있다")
    void chatIsPersisted() throws Exception {

        String token = signupAndLogin("chatlog@test.com", "기록유저");
        long liveId = startBroadcastAndGetLiveId(token);

        StompSession session = connect(token);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        subscribe(session, "/topic/lives/" + liveId, received);

        send(session, "/app/lives/" + liveId + "/chat", "{\"content\":\"저장되는 채팅\"}");
        poll(received);

        JsonNode history = get("/api/lives/" + liveId + "/chats", null);

        assertThat(history.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(history.path("data").path("content").get(0).path("content").asString())
                .isEqualTo("저장되는 채팅");

        session.disconnect();
    }

    @Test
    @DisplayName("로그인하지 않은 연결은 채팅을 보낼 수 없다")
    void anonymousCannotSendChat() throws Exception {

        String token = signupAndLogin("anonroom@test.com", "익명방주인");
        long liveId = startBroadcastAndGetLiveId(token);

        StompSession anonymous = connect(null);

        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        subscribe(anonymous, "/topic/lives/" + liveId, received);

        send(anonymous, "/app/lives/" + liveId + "/chat", "{\"content\":\"통과하면 안 됨\"}");

        // 아무것도 전달되지 않아야 한다
        assertThat(received.poll(3, TimeUnit.SECONDS)).isNull();

        JsonNode history = get("/api/lives/" + liveId + "/chats", null);
        assertThat(history.path("data").path("totalElements").asLong()).isZero();

        anonymous.disconnect();
    }

    @Test
    @DisplayName("채팅방 구독은 시청자 수로 집계돼 방송 정보에 반영된다")
    void viewerCountReflectsSubscribers() throws Exception {

        String token = signupAndLogin("viewer@test.com", "시청자집계");
        long liveId = startBroadcastAndGetLiveId(token);

        assertThat(get("/api/lives/" + liveId, null).path("data").path("viewerCount").asLong())
                .isZero();

        StompSession session = connect(token);

        BlockingQueue<String> viewerEvents = new LinkedBlockingQueue<>();
        subscribe(session, "/topic/lives/" + liveId + "/viewers", viewerEvents);
        subscribe(session, "/topic/lives/" + liveId, new LinkedBlockingQueue<>());

        JsonNode event = objectMapper.readTree(poll(viewerEvents));

        assertThat(event.path("liveId").asLong()).isEqualTo(liveId);
        assertThat(event.path("viewerCount").asLong()).isEqualTo(1);

        assertThat(get("/api/lives/" + liveId, null).path("data").path("viewerCount").asLong())
                .isEqualTo(1);

        session.disconnect();
    }

    // --- 도우미 ---

    private StompSession connect(String token) throws Exception {

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new SimpleMessageConverter());

        StompHeaders connectHeaders = new StompHeaders();

        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }

        return client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
                        new StompSessionHandlerAdapter() {
                        })
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private void subscribe(StompSession session, String destination, BlockingQueue<String> sink) {

        session.subscribe(destination, new StompFrameHandler() {

            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                sink.add(new String((byte[]) payload, StandardCharsets.UTF_8));
            }
        });
    }

    private void send(StompSession session, String destination, String jsonBody) {

        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        headers.setContentType(MimeTypeUtils.APPLICATION_JSON);

        session.send(headers, jsonBody.getBytes(StandardCharsets.UTF_8));
    }

    private String poll(BlockingQueue<String> queue) throws Exception {

        String value = queue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertThat(value).as("메시지를 받지 못했습니다").isNotNull();

        return value;
    }

    /** 회원을 직접 만들고 토큰을 발급한다. 다른 스레드에서도 보이도록 커밋된 상태여야 한다. */
    private String signupAndLogin(String email, String nickname) {

        com.sp.api.user.entity.User user =
                userRepository.save(new com.sp.api.user.entity.User(email, "encoded", nickname));

        return jwtProvider.createToken(user.getEmail());
    }

    /** RTMP 콜백 경로는 LiveIntegrationTest 에서 검증하므로 여기서는 서비스를 바로 호출한다. */
    private long startBroadcastAndGetLiveId(String token) {

        String email = jwtProvider.parseEmail(token);
        String streamKey = userRepository.findByEmail(email).orElseThrow().getStreamKey();

        return liveStreamService.startBroadcast(streamKey).getId();
    }

    private JsonNode get(String path, String token) {

        String body = restClient.get()
                .uri(path)
                .headers(headers -> {
                    if (token != null) {
                        headers.setBearerAuth(token);
                    }
                })
                .retrieve()
                .body(String.class);

        return objectMapper.readTree(body);
    }
}
