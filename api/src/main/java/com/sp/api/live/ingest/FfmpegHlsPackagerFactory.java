package com.sp.api.live.ingest;

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

        Files.createDirectories(properties.getHlsDir());

        Path playlist = properties.getHlsDir().resolve(streamName + ".m3u8");
        Path segments = properties.getHlsDir().resolve(streamName + "-%d.ts");

        List<String> command = List.of(
                properties.getFfmpegPath(),
                "-hide_banner", "-loglevel", "warning",
                "-fflags", "+genpts",
                "-i", "pipe:0",
                "-c:v", "copy",
                "-c:a", "aac", "-b:a", "128k",
                "-f", "hls",
                "-hls_time", "3",
                "-hls_list_size", "6",
                "-hls_flags", "delete_segments+independent_segments",
                "-hls_segment_filename", segments.toString(),
                playlist.toString()
        );

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

        Thread logger = new Thread(() -> drain(process, streamName), "ingest-ffmpeg-log");
        logger.setDaemon(true);
        logger.start();

        log.info("브라우저 방송 포장 시작: {}", streamName);

        return new FfmpegPackager(process, streamName);
    }

    private void drain(Process process, String streamName) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
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
