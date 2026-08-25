package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("로그인하면 액세스 토큰과 리프레시 토큰을 함께 받는다")
    void loginReturnsBothTokens() throws Exception {

        signup("refresh1@test.com", "리프레시1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"refresh1@test.com","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("리프레시 토큰으로 새 액세스 토큰을 발급받을 수 있다")
    void refreshIssuesNewAccessToken() throws Exception {

        signup("refresh2@test.com", "리프레시2");
        String refreshToken = loginAndGetRefreshToken("refresh2@test.com");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("리프레시 토큰은 한 번 쓰면 재사용할 수 없다")
    void refreshTokenIsSingleUse() throws Exception {

        signup("refresh3@test.com", "리프레시3");
        String refreshToken = loginAndGetRefreshToken("refresh3@test.com");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .andExpect(status().isOk());

        // 이미 회전(rotate)되어 소모된 토큰을 다시 쓰면 실패한다
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰으로는 재발급받을 수 없다")
    void refreshWithUnknownTokenFails() throws Exception {

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"존재하지-않는-토큰"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 그 리프레시 토큰은 더 이상 쓸 수 없다")
    void logoutInvalidatesRefreshToken() throws Exception {

        signup("refresh4@test.com", "리프레시4");
        String refreshToken = loginAndGetRefreshToken("refresh4@test.com");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃은 이미 없는 리프레시 토큰에 대해서도 조용히 성공한다")
    void logoutIsIdempotent() throws Exception {

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"이미-없는-토큰"}"""))
                .andExpect(status().isOk());
    }

    private String loginAndGetRefreshToken(String email) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = json(result).path("data").path("refreshToken").asString();

        assertThat(refreshToken).isNotBlank();

        return refreshToken;
    }
}
