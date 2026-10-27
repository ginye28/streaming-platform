package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 영상 길이(durationSeconds): 목록 카드의 길이 배지에 쓰는 값. */
class StreamDurationIntegrationTest extends IntegrationTestSupport {

    private MvcResult create(String token, String title, String videoUrl, String durationJson) throws Exception {
        return mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","videoUrl":"%s"%s}""".formatted(title, videoUrl, durationJson)))
                .andReturn();
    }

    @Test
    @DisplayName("길이를 주면 저장되고 목록과 상세에 나온다")
    void storesDuration() throws Exception {

        String owner = signupAndLogin("dur-owner1@test.com", "길이주인1");

        MvcResult created = create(owner, "길이있는영상", "/uploads/d1.mp4", ",\"durationSeconds\":225");
        org.assertj.core.api.Assertions.assertThat(created.getResponse().getStatus()).isEqualTo(201);

        long id = json(created).path("data").path("id").asLong();

        mockMvc.perform(get("/api/streams/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationSeconds").value(225));

        mockMvc.perform(get("/api/streams"))
                .andExpect(jsonPath("$.data.content[0].durationSeconds").value(225));
    }

    @Test
    @DisplayName("길이를 안 주면 null 이다(이미 올라간 영상, 길이를 못 읽은 영상)")
    void durationIsNullWhenNotGiven() throws Exception {

        String owner = signupAndLogin("dur-owner2@test.com", "길이주인2");
        long id = createStream(owner, "길이모르는영상");

        mockMvc.perform(get("/api/streams/" + id))
                .andExpect(jsonPath("$.data.durationSeconds").doesNotExist());
    }

    @Test
    @DisplayName("0 이하이거나 24시간을 넘는 길이는 400 이다")
    void rejectsOutOfRangeDuration() throws Exception {

        String owner = signupAndLogin("dur-owner3@test.com", "길이주인3");

        for (String bad : new String[]{"0", "-5", "86401"}) {
            org.assertj.core.api.Assertions.assertThat(
                    create(owner, "잘못된길이", "/uploads/d3.mp4", ",\"durationSeconds\":" + bad)
                            .getResponse().getStatus()).isEqualTo(400);
        }
    }

    @Test
    @DisplayName("수정할 때 같은 영상이면 길이를 안 줘도 그대로 두고, 다른 영상으로 바꾸면 비운다")
    void updateKeepsOrClearsDuration() throws Exception {

        String owner = signupAndLogin("dur-owner4@test.com", "길이주인4");
        long id = json(create(owner, "수정영상", "/uploads/d4.mp4", ",\"durationSeconds\":90"))
                .path("data").path("id").asLong();

        // 같은 영상, 길이 없음 → 유지
        mockMvc.perform(put("/api/streams/" + id)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"수정영상2","videoUrl":"/uploads/d4.mp4"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationSeconds").value(90));

        // 다른 영상, 길이 없음 → 옛 길이를 남기지 않는다
        mockMvc.perform(put("/api/streams/" + id)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"수정영상2","videoUrl":"/uploads/d4-new.mp4"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationSeconds").doesNotExist());

        // 다른 영상, 새 길이 → 새 길이
        mockMvc.perform(put("/api/streams/" + id)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"수정영상2","videoUrl":"/uploads/d4-newer.mp4","durationSeconds":300}"""))
                .andExpect(jsonPath("$.data.durationSeconds").value(300));
    }
}
