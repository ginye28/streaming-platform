package com.sp.api.live.ingest;

import com.sp.api.live.relay.FragmentListener;
import com.sp.api.live.relay.Fmp4Splitter;
import java.io.InputStream;
import java.util.ArrayList;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * ffmpeg 를 방송마다 하나씩 띄운다. 영상은 다시 인코딩하지 않고(copy) 포장만 바꾸므로 CPU 를 거의 안 쓴다.
 * 소리는 WebM 의 Opus 를 HLS 가 받는 AAC 로 바꾼다.
 *
 * 브라우저가 H.264 로 보내야 한다(화면에서 확인한다). 다른 코덱이면 ffmpeg 가 포장에 실패하고 방송이 끊긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FfmpegHlsPackagerFactory implements HlsPackagerFactory {

    /** 방송이 끝난 뒤에도 시청자가 마지막 조각을 받아 갈 수 있게 파일을 잠깐 둔다(초). */
    private static final long CLEANUP_DELAY_SECONDS = 60;

    /**
     * 수업(말하는 소리) 기준의 소리 설정. 모노 24kHz·48kbps 면 말은 또렷하고 AAC 인코딩 비용이 스테레오 48kHz 의 약 1/4 이다.
     * 무료 서버(0.1 CPU)에서 시청자 몫의 CPU 를 남기려는 것이다. 음악 방송이 필요해지면 환경변수로 올릴 수 있게 한다.
     */
    private static final String AUDIO_RATE = "24000";
    private static final String AUDIO_BITRATE = "48k";

    /** 저지연 조각 하나의 길이(마이크로초). 짧을수록 지연이 줄고 전송 횟수(CPU)가 는다. */
    private static final int FRAGMENT_MICROS = 100_000;

    private final IngestProperties properties;

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ingest-cleaner");
        t.setDaemon(true);
        return t;
    });

    /** 서버가 켜질 때 이전에 남은 조각을 비운다. */
    @PostConstruct
    void clearLeftovers() {
        try {
            Files.createDirectories(properties.getHlsDir());
            try (Stream<Path> files = Files.list(properties.getHlsDir())) {
                files.forEach(FfmpegHlsPackagerFactory::deleteQuietly);
            }
        } catch (IOException e) {
            log.warn("HLS 폴더를 비우지 못했습니다: {}", e.getMessage());
        }
    }

    @Override
    public HlsPackager open(String streamName) throws IOException {
        return open(streamName, null);
    }

    @Override
    public HlsPackager open(String streamName, FragmentListener listener) throws IOException {

        Files.createDirectories(properties.getHlsDir());

        Path playlist = properties.getHlsDir().resolve(streamName + ".m3u8");
        Path segments = properties.getHlsDir().resolve(streamName + "-%d.ts");

        List<String> command = listener == null
                ? hlsOnlyCommand(playlist, segments)
                : hlsAndFragmentsCommand(playlist, segments);

        // 표준 오류는 따로 읽는다(표준 출력은 fMP4 데이터다).
        Process process = new ProcessBuilder(command).start();

        Thread logger = new Thread(() -> drain(process, streamName), "ingest-ffmpeg-log");
        logger.setDaemon(true);
        logger.start();

        // 표준 출력은 시청자가 없어도 계속 비워야 한다. 안 비우면 파이프가 차서 ffmpeg 전체(HLS 포함)가 멈춘다.
        Thread pump = new Thread(() -> pumpFragments(process, streamName, listener), "ingest-ffmpeg-fmp4");
        pump.setDaemon(true);
        pump.start();

        log.info("브라우저 방송 포장 시작: {}", streamName);

        return new FfmpegPackager(process, streamName);
    }

    /** HLS 만 만든다(저지연 중계를 끈 서버). 영상은 복사, 소리는 AAC 로 바꾼다. */
    private List<String> hlsOnlyCommand(Path playlist, Path segments) {
        return new ArrayList<>(List.of(
                properties.getFfmpegPath(),
                "-hide_banner", "-loglevel", "warning",
                "-fflags", "+genpts+nobuffer",
                "-flags", "low_delay",
                "-probesize", "500000",
                "-analyzeduration", "500000",
                "-i", "pipe:0",
                "-c:v", "copy",
                "-c:a", "aac", "-ac", "1", "-ar", AUDIO_RATE, "-b:a", AUDIO_BITRATE,
                "-f", "hls",
                "-hls_time", "3",
                "-hls_list_size", "6",
                "-hls_flags", "delete_segments+independent_segments",
                "-hls_segment_filename", segments.toString(),
                playlist.toString()
        ));
    }

    /**
     * HLS 와 0.1초 단위 fMP4(표준 출력)를 한 번에 만든다.
     *
     * 소리(AAC)는 tee 로 한 번만 인코딩해 두 출력에 나눈다. 소리 인코딩이 이 작업의 CPU 대부분이라
     * (무료 서버는 0.1 CPU), 출력마다 따로 인코딩하면 시청자를 받기 전에 한도가 찬다.
     */
    private List<String> hlsAndFragmentsCommand(Path playlist, Path segments) {

        String hls = "[f=hls:hls_time=3:hls_list_size=6:hls_flags=delete_segments+independent_segments"
                + ":hls_segment_filename=" + segments + "]" + playlist;

        String fragments = "[f=mp4:movflags=+empty_moov+default_base_moof+frag_keyframe"
                + ":frag_duration=" + FRAGMENT_MICROS + ":flush_packets=1]pipe:1";

        return new ArrayList<>(List.of(
                properties.getFfmpegPath(),
                "-hide_banner", "-loglevel", "warning",
                // 들어오는 즉시 처리한다. 기본값은 입력을 몇 초씩 분석하며 모아서 첫 화면이 늦다.
                "-fflags", "+genpts+nobuffer",
                "-flags", "low_delay",
                "-probesize", "500000",
                "-analyzeduration", "500000",
                "-i", "pipe:0",
                "-map", "0:v:0", "-map", "0:a:0?",
                "-c:v", "copy",
                "-c:a", "aac", "-ac", "1", "-ar", AUDIO_RATE, "-b:a", AUDIO_BITRATE,
                "-f", "tee",
                hls + "|" + fragments
        ));
    }

    /** ffmpeg 표준 출력(fMP4)을 읽어 머리와 조각으로 나눠 listener 에 넘긴다. listener 가 없으면 그냥 비운다. */
    private void pumpFragments(Process process, String streamName, FragmentListener listener) {

        Fmp4Splitter splitter = listener == null ? null : new Fmp4Splitter(listener);
        byte[] buffer = new byte[64 * 1024];

        try (InputStream in = process.getInputStream()) {

            int read;

            while ((read = in.read(buffer)) != -1) {

                if (splitter == null) {
                    continue;
                }

                try {
                    splitter.feed(buffer, 0, read);
                } catch (RuntimeException e) {
                    // 시청자 쪽 문제가 ffmpeg 출력을 비우는 일을 멈추게 하면 안 된다(파이프가 차면 HLS 까지 멈춘다).
                    log.warn("저지연 조각 처리 중 오류. 이 방송은 HLS 로만 나갑니다: {} ({})", streamName, e.toString());
                    splitter = null;
                    continue;
                }

                if (splitter.isFailed()) {
                    log.warn("저지연 조각을 읽지 못해 이 방송은 HLS 로만 나갑니다: {}", streamName);
                    splitter = null;
                }
            }

        } catch (IOException ignored) {
            // 프로세스가 끝나면서 닫힌 것
        }
    }

    private void drain(Process process, String streamName) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.warn("ffmpeg[{}]: {}", streamName, line);
            }
        } catch (IOException ignored) {
            // 프로세스가 끝나면서 닫힌 것
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 임시 파일이라 못 지워도 다음에 지워진다
        }
    }

    private final class FfmpegPackager implements HlsPackager {

        private final Process process;
        private final String streamName;
        private final OutputStream stdin;
        private boolean closed;

        FfmpegPackager(Process process, String streamName) {
            this.process = process;
            this.streamName = streamName;
            this.stdin = process.getOutputStream();
        }

        @Override
        public synchronized void write(byte[] chunk) throws IOException {

            if (closed || !process.isAlive()) {
                throw new IOException("ffmpeg 가 종료되었습니다.");
            }

            stdin.write(chunk);
            stdin.flush();
        }

        @Override
        public synchronized void close() {

            if (closed) {
                return;
            }

            closed = true;

            try {
                stdin.close();
            } catch (IOException ignored) {
                // 이미 끊긴 것
            }

            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }

            cleaner.schedule(this::deleteFiles, CLEANUP_DELAY_SECONDS, TimeUnit.SECONDS);

            log.info("브라우저 방송 포장 종료: {}", streamName);
        }

        private void deleteFiles() {
            try (Stream<Path> files = Files.list(properties.getHlsDir())) {
                files.filter(p -> p.getFileName().toString().startsWith(streamName))
                        .forEach(FfmpegHlsPackagerFactory::deleteQuietly);
            } catch (IOException ignored) {
                // 임시 파일
            }
        }
    }
}
