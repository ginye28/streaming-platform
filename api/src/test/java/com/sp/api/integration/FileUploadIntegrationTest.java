package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileUploadIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("영상을 올리면 첫 장면을 뽑아 썸네일 주소도 같이 준다")
    void videoUploadReturnsGeneratedThumbnail() throws Exception {

        assumeTrue(ffmpegAvailable(), "ffmpeg 가 없어 건너뜁니다");

        String token = signupAndLogin("upload-video@test.com", "영상올리는사람");

        byte[] video;

        try (InputStream in = getClass().getResourceAsStream("/fixtures/sample.mp4")) {
            assertThat(in).isNotNull();
            video = in.readAllBytes();
        }

        mockMvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "clip.mp4", "video/mp4", video))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.endsWith(".mp4")))
                .andExpect(jsonPath("$.data.thumbnailUrl").value(org.hamcrest.Matchers.endsWith(".jpg")));
    }

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
    @DisplayName("이미지는 썸네일을 따로 뽑지 않는다")
    void imageUploadHasNoThumbnail() throws Exception {

        String token = signupAndLogin("upload-image@test.com", "그림올리는사람");

        mockMvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.endsWith(".png")))
                .andExpect(jsonPath("$.data.thumbnailUrl").doesNotExist());
    }

    @Test
    @DisplayName("확장자가 없는 파일은 400 이다")
    void rejectsFileWithoutExtension() throws Exception {

        String token = signupAndLogin("upload-bad@test.com", "확장자없는사람");

        mockMvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "noext", "application/octet-stream", new byte[]{1}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
