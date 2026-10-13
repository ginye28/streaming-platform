package com.sp.api.integration;

import com.sp.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보안 보강: 대입 공격 제한, 비밀번호 변경 뒤 토큰 폐기, 링크 주소 검증.
 * 제한은 평소 테스트에서는 꺼 두므로(같은 IP 로 가입을 수없이 한다) 여기서만 켠다.
 */
@TestPropertySource(properties = {
        "app.security.rate-limit.enabled=true",
        "app.security.rate-limit.signup-per-10min=3",
})
class SecurityHardeningIntegrationTest extends IntegrationTestSupport {

    private void createUser(String email, String nickname) {
        userRepository.saveAndFlush(new User(email, passwordEncoder.encode("password123"), nickname));
    }

    private MvcResult tryLogin(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, password)))
                .andReturn();
    }

    @Test
    @DisplayName("한 계정에서 로그인을 열 번 틀리면 맞는 비밀번호로도 잠시 막힌다")
    void locksAccountAfterRepeatedFailures() throws Exception {

        createUser("lock-me@test.com", "잠기는사람");

        for (int i = 0; i < 10; i++) {
            assertThat(tryLogin("lock-me@test.com", "wrong-password-" + i).getResponse().getStatus())
                    .isEqualTo(401);
        }

        MvcResult locked = tryLogin("lock-me@test.com", "password123");

        assertThat(locked.getResponse().getStatus()).isEqualTo(429);
        assertThat(locked.getResponse().getContentAsString()).contains("다시 시도");
    }

    @Test
    @DisplayName("같은 곳에서 가입을 너무 자주 하면 429 와 다시 시도할 시간을 준다")
    void limitsSignupsPerIp() throws Exception {

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/signup")
                            .header("CF-Connecting-IP", "203.0.113.7")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"ip-limit-%d@test.com","password":"password123","nickname":"제한%d"}"""
                                    .formatted(i, i)))
                    .andExpect(status().isCreated());
        }

        MvcResult blocked = mockMvc.perform(post("/api/auth/signup")
                        .header("CF-Connecting-IP", "203.0.113.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ip-limit-x@test.com","password":"password123","nickname":"제한x"}"""))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        assertThat(blocked.getResponse().getHeader("Retry-After")).isNotBlank();

        // 다른 곳에서 오는 요청은 영향이 없다.
        mockMvc.perform(post("/api/auth/signup")
                        .header("CF-Connecting-IP", "203.0.113.8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ip-other@test.com","password":"password123","nickname":"다른곳"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 이미 발급된 리프레시 토큰으로는 더 이어 갈 수 없다")
    void changingPasswordRevokesRefreshTokens() throws Exception {

        createUser("pw-revoke@test.com", "토큰폐기");

        MvcResult login = tryLogin("pw-revoke@test.com", "password123");
        assertThat(login.getResponse().getStatus()).isEqualTo(200);

        String access = json(login).path("data").path("accessToken").asString();
        String refresh = json(login).path("data").path("refreshToken").asString();

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"new-password-456"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(refresh)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("크레딧 링크에 javascript: 같은 주소는 넣을 수 없다")
    void rejectsScriptUrlInCreditLink() throws Exception {

        createUser("xss-link@test.com", "링크주인");
        String token = login("xss-link@test.com");

        for (String bad : new String[]{"javascript:alert(1)", "JaVaScRiPt:alert(1)", " javascript:alert(1)",
                "data:text/html,<script>alert(1)</script>", "//evil.example/x", "ftp://example.com/a"}) {

            mockMvc.perform(put("/api/users/me/channel-profile")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of(
                                    "credits", java.util.List.of(java.util.Map.of(
                                            "role", "ILLUSTRATOR", "name", "그림작가", "link", bad))
                            ))))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(put("/api/users/me/channel-profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credits":[{"role":"ILLUSTRATOR","name":"그림작가","link":"https://x.com/a"}]}"""))
                .andExpect(status().isOk());
    }
}
