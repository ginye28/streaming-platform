package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("영상 목록은 비로그인도 조회할 수 있다")
    void streamListIsPublic() throws Exception {

        mockMvc.perform(get("/api/streams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("인증 없이 쓰기 요청하면 JSON 401 을 받는다")
    void writeWithoutTokenReturns401() throws Exception {

        mockMvc.perform(post("/api/streams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","videoUrl":"/uploads/a.mp4"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    @Test
    @DisplayName("없는 영상 조회는 404 를 반환한다")
    void missingStreamReturns404() throws Exception {

        mockMvc.perform(get("/api/streams/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("필수 쿼리 파라미터 누락은 500 이 아니라 400 이다")
    void missingRequestParamReturns400() throws Exception {

        mockMvc.perform(get("/api/streams/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("검증 실패는 어떤 필드가 틀렸는지 알려준다")
    void invalidSignupReturns400() throws Exception {

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"123","nickname":"a"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("중복 이메일 회원가입은 409 를 반환한다")
    void duplicateSignupReturns409() throws Exception {

        signup("dup@test.com", "중복이");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@test.com","password":"password123","nickname":"다른이름"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인은 401 을 반환한다")
    void wrongPasswordReturns401() throws Exception {

        signup("login@test.com", "로그인");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@test.com","password":"wrongpassword"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("가입 → 로그인 → 영상 등록 → 좋아요 토글이 이어서 동작한다")
    void fullFlow() throws Exception {

        signup("flow@test.com", "플로우");
        String token = login("flow@test.com");

        MvcResult created = mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"내 영상","description":"설명","videoUrl":"/uploads/a.mp4"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nickname").value("플로우"))
                .andReturn();

        long streamId = json(created).path("data").path("id").asLong();

        // 상세 조회 시 조회수가 올라간다
        mockMvc.perform(get("/api/streams/" + streamId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));

        // 좋아요 → 취소
        mockMvc.perform(post("/api/streams/" + streamId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likesCount").value(1));

        mockMvc.perform(post("/api/streams/" + streamId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likesCount").value(0));
    }

    @Test
    @DisplayName("남의 영상은 수정할 수 없다")
    void cannotUpdateOthersStream() throws Exception {

        signup("owner2@test.com", "주인");
        signup("intruder@test.com", "침입자");

        String ownerToken = login("owner2@test.com");
        String intruderToken = login("intruder@test.com");

        MvcResult created = mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"내 것","videoUrl":"/uploads/a.mp4"}"""))
                .andExpect(status().isCreated())
                .andReturn();

        long streamId = json(created).path("data").path("id").asLong();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/streams/" + streamId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"뺏었다","videoUrl":"/uploads/b.mp4"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("수정 권한이 없습니다."));
    }

    @Test
    @DisplayName("위조된 토큰은 인증되지 않는다")
    void forgedTokenIsRejected() throws Exception {

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    private void signup(String email, String nickname) throws Exception {

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","nickname":"%s"}"""
                                .formatted(email, nickname)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        String token = json(result).path("data").path("accessToken").asString();

        assertThat(token).isNotBlank();

        return token;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
