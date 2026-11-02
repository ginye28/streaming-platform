package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 시청 화면의 재생 방식 보고(비로그인 가능)와 관리자 집계 조회. */
class PlaybackStatsIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("비로그인 시청자도 재생 방식을 보고할 수 있고, 관리자는 그 숫자를 본다")
    void reportsAreCountedForAdmin() throws Exception {

        String admin = adminToken("playback-admin@test.com", "재생관리자");

        mockMvc.perform(post("/api/playback/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"LOW_LATENCY"}"""))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/playback/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"HLS","reason":"UNSUPPORTED"}"""))
                .andExpect(status().isNoContent());

        // 이유가 없는 HLS 는 OTHER 로 센다
        mockMvc.perform(post("/api/playback/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mode":"HLS"}"""))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/playback").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportedLowLatency").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.reportedHls").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.data.hlsReasons.UNSUPPORTED").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.hlsReasons.OTHER").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.lowLatencyViewersNow").value(0));
    }

    @Test
    @DisplayName("정해진 목록에 없는 방식·이유나 빈 본문은 400 으로 거절한다")
    void unknownValuesAreRejected() throws Exception {

        for (String body : new String[]{
                "{\"mode\":\"FAST\"}",
                "{\"mode\":\"HLS\",\"reason\":\"<script>alert(1)</script>\"}",
                "{}",
        }) {
            mockMvc.perform(post("/api/playback/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("집계 조회는 관리자만 할 수 있다")
    void snapshotIsAdminOnly() throws Exception {

        String user = signupAndLogin("playback-user@test.com", "재생일반");

        mockMvc.perform(get("/api/admin/playback"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/playback").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden());
    }
}
