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
 * 일반 요청 속도 제한(RateLimitFilter)을 켠 서버. 한도를 작게 줄여 놓고 넘치면 막히는지, 다른 사람과 미디어는
 * 안 막히는지 본다. 로그인·가입·갱신 제한과 계정 잠금은 SecurityHardeningIntegrationTest 가 본다.
 *
 * 제한은 메모리에 있어 테스트끼리 공유되므로, 테스트마다 다른 IP(remoteAddr)를 쓴다.
 */
@TestPropertySource(properties = {
        "app.security.rate-limit.enabled=true",
        "app.security.rate-limit.read-per-minute=5",
        "app.security.rate-limit.write-per-minute=4",
        "app.security.rate-limit.upload-per-minute=2",
})
class RateLimitIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("조회를 한도보다 많이 하면 429 와 Retry-After 로 막고, 다른 IP 는 영향이 없다")
    void readLimitIsPerIp() throws Exception {

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/streams").with(remoteAddr("10.3.0.1")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/streams").with(remoteAddr("10.3.0.1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("너무 많아요")));

        mockMvc.perform(get("/api/streams").with(remoteAddr("10.3.0.2")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("영상 조각(live-hls)은 같은 IP 가 한도를 넘겼어도 제한하지 않는다")
    void mediaFilesAreNotLimited() throws Exception {

        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/streams").with(remoteAddr("10.3.1.1")));
        }

        mockMvc.perform(get("/api/streams").with(remoteAddr("10.3.1.1")))
                .andExpect(status().isTooManyRequests());

        // 없는 파일이라 404 이지만 429 는 아니다
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/live-hls/none.m3u8").with(remoteAddr("10.3.1.1")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("쓰기 요청은 조회와 따로 센다")
    void writesAreCountedSeparately() throws Exception {

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/streams")
                            .with(remoteAddr("10.4.0.1"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/streams")
                        .with(remoteAddr("10.4.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests());

        // 같은 IP 의 조회는 아직 남아 있다
        mockMvc.perform(get("/api/streams").with(remoteAddr("10.4.0.1")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("파일 업로드는 쓰기보다 더 적게 허용한다")
    void uploadHasItsOwnLimit() throws Exception {

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/files/upload").with(remoteAddr("10.4.1.1")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/files/upload").with(remoteAddr("10.4.1.1")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("막힌 기록은 관리자가 볼 수 있다")
    void adminCanSeeBlockedRequests() throws Exception {

        String admin = adminToken("sec-admin@test.com", "보안관리자");

        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/streams").with(remoteAddr("10.5.0.1")));
        }

        var result = mockMvc.perform(get("/api/admin/security/events")
                        .with(remoteAddr("10.5.0.2"))
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
                        .with(remoteAddr("10.6.0.1"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
