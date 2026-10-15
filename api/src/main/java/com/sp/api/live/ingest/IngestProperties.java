package com.sp.api.live.ingest;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/** 브라우저에서 바로 방송하는 기능(웹 → API 의 ffmpeg → HLS)의 설정. */
@Getter
@Component
public class IngestProperties {

    /** 끄면 브라우저 방송 연결을 받지 않는다. ffmpeg 가 없는 환경에서는 끈다. */
    private final boolean enabled;

    /** 만든 HLS 조각과 재생목록이 놓이는 곳. 서버가 켜질 때 비운다. 임시 디스크라 재배포하면 사라져도 된다. */
    private final Path hlsDir;

    private final String ffmpegPath;

    /** 동시에 받을 수 있는 방송 수. 무료 서버는 CPU 가 작아서 적게 둔다. */
    private final int maxSessions;

    /** 한 방송이 보낼 수 있는 최대 속도(kbps). 넘기면 끊는다. 화면에서는 2500 으로 보낸다. */
    private final int maxKbps;

    /** 끄면 HLS 로만 내보낸다(저지연 중계 없음). 무료 서버가 못 버티면 끄는 스위치. */
    private final boolean lowLatencyEnabled;

    /**
     * 저지연으로 동시에 받는 시청자 수의 상한. 넘으면 HLS 로 보낸다(HLS 는 파일을 내려 주는 일이라 훨씬 싸다).
     * 시청자 한 명이 전송 스레드와 큐로 메모리를 쓰므로, 512MB 서버에서 50명이 한계에 닿았다(부하 시험).
     */
    private final int lowLatencyMaxViewers;

    public IngestProperties(
            @Value("${app.ingest.enabled:true}") boolean enabled,
            @Value("${app.ingest.hls-dir:${java.io.tmpdir}/sp-ingest-hls}") String hlsDir,
            @Value("${file.thumbnail.ffmpeg-path:ffmpeg}") String ffmpegPath,
            @Value("${app.ingest.max-sessions:1}") int maxSessions,
            @Value("${app.ingest.max-kbps:6000}") int maxKbps,
            @Value("${app.ingest.low-latency-enabled:true}") boolean lowLatencyEnabled,
            @Value("${app.ingest.low-latency-max-viewers:30}") int lowLatencyMaxViewers
    ) {
        this.enabled = enabled;
        this.hlsDir = Paths.get(hlsDir).toAbsolutePath().normalize();
        this.ffmpegPath = ffmpegPath;
        this.maxSessions = maxSessions;
        this.maxKbps = maxKbps;
        this.lowLatencyEnabled = lowLatencyEnabled;
        this.lowLatencyMaxViewers = lowLatencyMaxViewers;
    }
}
