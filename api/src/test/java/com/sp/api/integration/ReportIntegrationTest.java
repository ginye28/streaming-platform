package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("영상을 신고할 수 있다")
    void reportStream() throws Exception {

        String reporterToken = signupAndLogin("reporter1@test.com", "신고자1");
        String targetToken = signupAndLogin("reportedowner1@test.com", "피신고자1");
        long streamId = createStream(targetToken, "문제의영상");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"STREAM","targetId":%d,"reason":"부적절한 내용입니다."}"""
                                .formatted(streamId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.targetType").value("STREAM"));
    }

    @Test
    @DisplayName("존재하지 않는 영상은 신고할 수 없다")
    void reportUnknownTargetFails() throws Exception {

        String reporterToken = signupAndLogin("reporter2@test.com", "신고자2");

        mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"STREAM","targetId":9999,"reason":"사유"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("일반 사용자는 신고 목록을 볼 수 없다")
    void nonAdminCannotListReports() throws Exception {

        String token = signupAndLogin("reporter3@test.com", "신고자3");

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자는 신고 목록을 조회하고 상태를 바꿀 수 있다")
    void adminCanListAndResolveReports() throws Exception {

        String reporterToken = signupAndLogin("reporter4@test.com", "신고자4");
        String targetToken = signupAndLogin("reportedowner4@test.com", "피신고자4");
        long streamId = createStream(targetToken, "신고당한영상");

        MvcResult reportResult = mockMvc.perform(post("/api/reports")
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType":"STREAM","targetId":%d,"reason":"사유"}"""
                                .formatted(streamId)))
                .andExpect(status().isCreated())
                .andReturn();

        long reportId = json(reportResult).path("data").path("id").asLong();

        String adminTok = adminToken("report-admin@test.com", "신고관리자");

        mockMvc.perform(get("/api/admin/reports")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(get("/api/admin/reports")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(patch("/api/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(get("/api/admin/reports")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
