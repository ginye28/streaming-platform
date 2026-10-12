package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 요청 속도 제한을 켠 서버. 한도를 작게 줄여 놓고, 넘치면 막히는지와 정상 사용자는 안 막히는지 본다.
 * 속도 제한은 메모리에 있어 테스트끼리 공유되므로, 테스트마다 다른 IP(X-Forwarded-For)를 쓴다.
 */
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.auth-per-minute=3",
        "app.rate-limit.login-per-email=2",
        "app.rate-limit.read-per-minute=5",
        "app.rate-limit.write-per-minute=4",
})
class RateLimitIntegrationTest extends IntegrationTestSupport {

    private static final String BAD_LOGIN = """
            {"email":"nobody@test.com","password":"wrong-password"}""";

    @Test
    @DisplayName("로그인을 한도보다 많이 두드리면 429 와 Retry-After 로 막는다")
    void loginIsRateLimitedPerIp() throws Exception {

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "10.1.0.1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"user%d@test.com","password":"wrong-password"}""".formatted(i)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.1.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BAD_LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("너무 많아요")));

        // 다른 사람(다른 IP)은 영향이 없다
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.1.0.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BAD_LOGIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("IP 를 바꿔 가며 같은 계정을 두드려도 한 계정에는 정해진 횟수만 허용한다")
    void loginIsRateLimitedPerAccount() throws Exception {

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "10.2.0." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"target@test.com","password":"wrong-%d"}""".formatted(i)))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "10.2.0.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"TARGET@test.com","password":"wrong-again"}"""))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("로그인을 너무 많이")));
    }

    @Test
    @DisplayName("조회도 한도를 넘으면 막지만, 영상 조각(live-hls)은 제한하지 않는다")
    void readLimitDoesNotApplyToMediaFiles() throws Exception {

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/streams").header("X-Forwarded-For", "10.3.0.1"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/streams").header("X-Forwarded-For", "10.3.0.1"))
                .andExpect(status().isTooManyRequests());

        // 같은 IP 라도 영상 조각은 계속 받을 수 있다(없는 파일이라 404 이지만 429 는 아니다)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/live-hls/none.m3u8").header("X-Forwarded-For", "10.3.0.1"))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("쓰기 요청은 조회와 따로 센다")
    void writesAreCountedSeparately() throws Exception {

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/streams")
                            .header("X-Forwarded-For", "10.4.0.1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/streams")
                        .header("X-Forwarded-For", "10.4.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests());

        // 같은 IP 의 조회는 아직 남아 있다
        mockMvc.perform(get("/api/streams").header("X-Forwarded-For", "10.4.0.1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("막힌 기록은 관리자가 볼 수 있다")
    void adminCanSeeBlockedRequests() throws Exception {

        String admin = adminToken("sec-admin@test.com", "보안관리자");

        // 한도 5 의 조회를 넘겨 막힌 기록을 만든다
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/streams").header("X-Forwarded-For", "10.5.0.1"));
        }

        var result = mockMvc.perform(get("/api/admin/security/events")
                        .header("X-Forwarded-For", "10.5.0.2")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn();

        var data = json(result).path("data");

        assertThat(data.path("counts").path("RATE_LIMITED").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(data.path("recent").size()).isGreaterThanOrEqualTo(1);
        assertThat(data.path("recent").get(0).path("ip").asString()).isNotBlank();
    }

    @Test
    @DisplayName("관리자가 아니면 기록을 볼 수 없다")
    void securityEventsAreAdminOnly() throws Exception {

        String token = signupAndLogin("sec-user@test.com", "일반유저");

        mockMvc.perform(get("/api/admin/security/events")
                        .header("X-Forwarded-For", "10.6.0.1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
