package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 글·주소 입력 필터와 응답 보안 헤더. */
class InputFilteringIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("댓글에 HTML 태그가 있으면 거절하고, 부등호만 있는 글은 저장한다")
    void commentMarkupIsRejected() throws Exception {

        String token = signupAndLogin("filter-comment@test.com", "필터댓글");
        long streamId = createStream(token, "필터 영상");

        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"<script>alert(document.cookie)</script>"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("HTML 태그")));

        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"3 < 5 이고 <3 사랑해요"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("너무 긴 제목은 500 이 아니라 400 으로 알려 준다")
    void overlongTitleIsBadRequest() throws Exception {

        String token = signupAndLogin("filter-long@test.com", "긴제목");

        mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","videoUrl":"/uploads/a.mp4"}""".formatted("가".repeat(101))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("영상·썸네일 주소에 javascript: 같은 스킴은 쓸 수 없다")
    void dangerousUrlIsRejected() throws Exception {

        String token = signupAndLogin("filter-url@test.com", "주소필터");

        mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","videoUrl":"javascript:alert(1)"}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","videoUrl":"/uploads/a.mp4","thumbnailUrl":"data:text/html;base64,PHNjcmlwdD4="}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","videoUrl":"https://example.com/v.mp4","thumbnailUrl":"/uploads/t.jpg"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("채널 크레딧 링크에 javascript: 를 넣을 수 없다")
    void creditLinkIsFiltered() throws Exception {

        String token = signupAndLogin("filter-credit@test.com", "크레딧필터");

        mockMvc.perform(put("/api/users/me/channel-profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credits":[{"role":"ILLUSTRATOR","name":"그림","link":"javascript:alert(1)"}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("닉네임에도 태그를 넣을 수 없다")
    void nicknameIsFiltered() throws Exception {

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"filter-nick@test.com","password":"password123","nickname":"<b>굵게</b>"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("거절된 입력은 관리자가 볼 수 있는 보안 기록에 남는다")
    void rejectedInputIsRecorded() throws Exception {

        String admin = adminToken("filter-admin@test.com", "필터관리자");
        String token = signupAndLogin("filter-rec@test.com", "기록필터");
        long streamId = createStream(token, "기록 영상");

        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"<iframe src=//evil></iframe>"}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/security/events").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.INPUT_REJECTED").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("응답에는 브라우저 방어 헤더가 붙고, API 응답은 어떤 스크립트도 실행하지 못한다")
    void securityHeadersAreSet() throws Exception {

        mockMvc.perform(get("/api/streams"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy",
                        org.hamcrest.Matchers.containsString("default-src 'none'")))
                .andExpect(header().string("Permissions-Policy",
                        org.hamcrest.Matchers.containsString("camera=()")));
    }

    @Test
    @DisplayName("Swagger 화면에는 스크립트를 막는 CSP 를 붙이지 않는다")
    void swaggerKeepsWorking() throws Exception {

        mockMvc.perform(get("/swagger"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }
}
