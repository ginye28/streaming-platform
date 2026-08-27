package com.sp.api.common.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class VideoThumbnailServiceTest {

    @TempDir
    Path uploadDir;

    private VideoThumbnailService service(boolean enabled, String ffmpegPath) {
        return new VideoThumbnailService(enabled, ffmpegPath, 20);
    }

    /** 테스트용 2초짜리 영상을 임시 폴더에 풀어 놓는다. */
    private Path sampleVideo() throws Exception {

        Path target = uploadDir.resolve("sample.mp4");

        try (InputStream in = getClass().getResourceAsStream("/fixtures/sample.mp4")) {
            assertThat(in).isNotNull();
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return target;
    }

    /** ffmpeg 가 깔려 있지 않은 곳에서는 뽑아내는 시험을 건너뛴다. */
    private boolean ffmpegAvailable() {
        try {
            return new ProcessBuilder("ffmpeg", "-version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("영상에서 썸네일을 뽑아 영상 옆에 저장한다")
    void generatesThumbnail() throws Exception {

        assumeTrue(ffmpegAvailable(), "ffmpeg 가 없어 건너뜁니다");

        Path video = sampleVideo();

        String name = service(true, "ffmpeg").generate(video, "sample");

        assertThat(name).isEqualTo("sample.jpg");
        assertThat(uploadDir.resolve("sample.jpg")).exists();
        assertThat(Files.size(uploadDir.resolve("sample.jpg"))).isPositive();
    }

    @Test
    @DisplayName("ffmpeg 를 찾을 수 없으면 파일을 남기지 않고 null 을 돌려준다")
    void returnsNullWhenFfmpegIsMissing() throws Exception {

        Path video = sampleVideo();

        assertThat(service(true, "no-such-ffmpeg-binary").generate(video, "sample")).isNull();
        assertThat(uploadDir.resolve("sample.jpg")).doesNotExist();
    }

    @Test
    @DisplayName("꺼 두면 ffmpeg 를 부르지 않는다")
    void doesNothingWhenDisabled() throws Exception {

        Path video = sampleVideo();

        assertThat(service(false, "ffmpeg").generate(video, "sample")).isNull();
        assertThat(uploadDir.resolve("sample.jpg")).doesNotExist();
    }

    @Test
    @DisplayName("영상 확장자만 썸네일 대상으로 본다")
    void tellsVideoExtensionsApart() {

        VideoThumbnailService service = service(true, "ffmpeg");

        assertThat(service.isVideo("mp4")).isTrue();
        assertThat(service.isVideo("MOV")).isTrue();
        assertThat(service.isVideo("png")).isFalse();
        assertThat(service.isVideo(null)).isFalse();
    }
}
