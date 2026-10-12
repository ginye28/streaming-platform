package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 무료 서버는 재시작하면 업로드 폴더가 비워진다. 그래도 올린 파일이 깨지지 않는지 확인한다.
 */
class UploadPersistenceIntegrationTest extends IntegrationTestSupport {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Test
    @DisplayName("디스크에서 파일이 사라져도 DB 사본에서 되살려 그대로 내려준다")
    void restoresFileDeletedFromDisk() throws Exception {

        String token = signupAndLogin("persist-upload@test.com", "파일지키는사람");

        // 1MB 조각이 세 개 나오도록 2.5MB 로 올린다. 마지막 조각은 짧다.
        byte[] bytes = new byte[2_500_000];
        new Random(42).nextBytes(bytes);

        String url = objectMapper.readTree(
                mockMvc.perform(multipart("/api/files/upload")
                                .file(new MockMultipartFile("file", "mark.png", "image/png", bytes))
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        ).path("data").path("url").asText();

        Path onDisk = Paths.get(uploadDir).toAbsolutePath().normalize()
                .resolve(url.substring("/uploads/".length()));

        assertThat(onDisk).exists();

        // 서버가 재시작해서 업로드 폴더가 비워진 상황.
        Files.delete(onDisk);

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes));

        assertThat(onDisk).exists();
    }

    @Test
    @DisplayName("올린 적 없는 파일은 그대로 404 다")
    void unknownFileIsStillNotFound() throws Exception {

        mockMvc.perform(get("/uploads/never-uploaded.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("경로를 벗어나는 이름은 되살리기를 시도하지 않고 404 다")
    void traversalNameIsNotFound() throws Exception {

        mockMvc.perform(get("/uploads/..%2F..%2Fapplication.yaml"))
                .andExpect(status().is4xxClientError());
    }
}
