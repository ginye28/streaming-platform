package com.sp.api.common.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 올라온 영상에서 한 장면을 뽑아 썸네일로 저장한다.
 *
 * ffmpeg 는 있을 수도 없을 수도 있는 바깥 프로그램이라, 실패하면 그냥 null 을 돌려준다.
 * 썸네일이 없다고 해서 영상 업로드까지 막을 이유는 없다.
 */
@Slf4j
@Service
public class VideoThumbnailService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "webm", "m4v", "mkv");

    /** 맨 앞은 검은 화면인 경우가 많아 1초 지점을 먼저 시도한다. 그보다 짧은 영상이면 맨 앞으로 물러선다. */
    private static final List<String> SEEK_CANDIDATES = List.of("00:00:01", "00:00:00");

    /**
     * 메모리를 아끼는 방식을 먼저, 안 되면 평범한 방식으로 시도한다.
     *
     * 아이폰 같은 4K HEVC 영상 한 장을 뽑는 데 ffmpeg 가 기본 설정으로 500MB 넘게 쓰는 것을 확인했다
     * (디코더가 코어 수만큼 스레드와 프레임 버퍼를 잡는다). 서버(무료 512MB)가 이 한 번으로 죽었다.
     *  - -threads 1       스레드를 하나로. 이것만으로 약 160MB.
     *  - -skip_frame nokey 키프레임만 디코딩. 합쳐서 약 50MB.
     * 키프레임만 읽는 방식이 안 맞는 파일이 있을 수 있어서 평범한 방식(스레드만 1개)을 뒤에 둔다.
     */
    private static final List<List<String>> DECODE_MODES = List.of(
            List.of("-threads", "1", "-skip_frame", "nokey"),
            List.of("-threads", "1")
    );

    /** 영상 길이로 허용하는 최대(초). 이보다 길게 나오면 잘못 읽은 것으로 보고 길이를 비워 둔다. */
    public static final int MAX_DURATION_SECONDS = 86_400;

    private final String ffmpegPath;
    private final String ffprobePath;
    private final boolean enabled;
    private final long timeoutSeconds;

    public VideoThumbnailService(
            @Value("${file.thumbnail.enabled:true}") boolean enabled,
            @Value("${file.thumbnail.ffmpeg-path:ffmpeg}") String ffmpegPath,
            @Value("${file.thumbnail.timeout-seconds:20}") long timeoutSeconds
    ) {
        this.enabled = enabled;
        this.ffmpegPath = ffmpegPath;
        // ffmpeg 와 같은 꾸러미로 깔리는 ffprobe 를 같은 폴더에서 찾는다.
        this.ffprobePath = ffmpegPath.endsWith("ffmpeg")
                ? ffmpegPath.substring(0, ffmpegPath.length() - "ffmpeg".length()) + "ffprobe"
                : "ffprobe";
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isVideo(String extension) {
        return extension != null && VIDEO_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * 영상 옆에 같은 이름의 .jpg 를 만든다.
     *
     * @return 만들어진 파일 이름. 못 만들었으면 null.
     */
    public String generate(Path video, String baseName) {

        if (!enabled) {
            return null;
        }

        String thumbnailName = baseName + ".jpg";
        Path target = video.resolveSibling(thumbnailName);

        for (List<String> mode : DECODE_MODES) {
            for (String seek : SEEK_CANDIDATES) {
                if (run(video, target, seek, mode)) {
                    return thumbnailName;
                }
            }
        }

        deleteQuietly(target);

        return null;
    }

    /**
     * 영상의 길이(초, 반올림)를 읽는다. ffprobe 가 없거나 읽지 못하면 null 이다.
     * 길이가 없다고 업로드를 막지 않는다. 화면에서 길이 배지만 안 보일 뿐이다.
     */
    public Integer probeDurationSeconds(Path video) {

        if (!enabled) {
            return null;
        }

        ProcessBuilder builder = new ProcessBuilder(
                ffprobePath, "-v", "error", "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1", video.toString());

        builder.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = null;

        try {
            process = builder.start();

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("영상 길이 읽기가 {}초를 넘겨 중단했습니다: {}", timeoutSeconds, video.getFileName());
                return null;
            }

            if (process.exitValue() != 0) {
                return null;
            }

            String output = new String(process.getInputStream().readAllBytes()).trim();
            double seconds = Double.parseDouble(output);
            long rounded = Math.round(seconds);

            return rounded >= 1 && rounded <= MAX_DURATION_SECONDS ? (int) rounded : null;

        } catch (IOException | NumberFormatException e) {
            log.debug("영상 길이를 읽지 못했습니다: {}", e.getMessage());
            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;

        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /** 한 장면을 뽑아 본다. 만들어진 파일이 비어 있지 않을 때만 성공으로 친다. */
    private boolean run(Path video, Path target, String seek, List<String> decodeMode) {

        List<String> command = new java.util.ArrayList<>();
        command.add(ffmpegPath);
        command.addAll(List.of("-y", "-nostdin", "-loglevel", "error"));
        // 입력 쪽 옵션(디코더). -i 보다 앞에 있어야 디코더에 적용된다.
        command.addAll(decodeMode);
        // -ss 를 -i 앞에 두면 훑지 않고 바로 그 지점으로 건너뛴다.
        command.addAll(List.of("-ss", seek, "-i", video.toString()));
        // 소리·자막·데이터는 필요 없다. 한 장만 뽑는다.
        command.addAll(List.of("-an", "-sn", "-dn", "-frames:v", "1"));
        // 너무 큰 원본이 그대로 썸네일이 되지 않게 가로를 640 으로 맞춘다.
        // -2 는 세로를 비율대로 두되 짝수로 맞추라는 뜻이다.
        command.addAll(List.of("-vf", "scale=640:-2", "-q:v", "3", target.toString()));

        ProcessBuilder builder = new ProcessBuilder(command);

        // ffmpeg 의 진행 로그는 버린다. 붙잡고 읽으면 그 읽기에 갇혀서
        // 아래 시간 제한이 걸리지 않는다.
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = null;

        try {
            process = builder.start();

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("썸네일 생성이 {}초를 넘겨 중단했습니다: {}", timeoutSeconds, video.getFileName());
                return false;
            }

            if (process.exitValue() != 0) {
                log.debug("ffmpeg 가 {} 로 끝났습니다 (건너뛴 지점 {})", process.exitValue(), seek);
                return false;
            }

            return Files.exists(target) && Files.size(target) > 0;

        } catch (IOException e) {
            // ffmpeg 가 설치돼 있지 않은 경우도 여기로 온다.
            log.warn("썸네일을 만들지 못했습니다. ffmpeg 를 찾을 수 있는지 확인해 주세요: {}", e.getMessage());
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;

        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.debug("남은 썸네일 파일을 지우지 못했습니다: {}", path);
        }
    }
}
