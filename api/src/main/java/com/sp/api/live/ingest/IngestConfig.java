package com.sp.api.live.ingest;

import com.sp.api.live.service.LiveStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/** 브라우저 방송 연결 주소(/ingest) 등록. */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class IngestConfig implements WebSocketConfigurer {

    private final IngestWebSocketHandler handler;
    private final LiveStreamService liveStreamService;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ingest")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }

    /** 서버가 꺼졌다 켜지면 브라우저 방송은 끊겼다. 방송 중으로 남은 것을 닫는다. */
    @EventListener(ApplicationReadyEvent.class)
    public void closeStaleBroadcasts() {
        int closed = liveStreamService.closeStaleBrowserBroadcasts();

        if (closed > 0) {
            log.info("끊긴 브라우저 방송 {}건을 정리했습니다.", closed);
        }
    }
}
