package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("카테고리 목록은 비로그인도 조회할 수 있다")
    void findAllIsPublic() throws Exception {

        String token = adminToken("cat-admin1@test.com", "카테관리자1");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"저스트채팅"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("저스트채팅"));
    }

    @Test
    @DisplayName("관리자는 카테고리를 만들 수 있다")
    void adminCanCreate() throws Exception {

        String token = adminToken("cat-admin2@test.com", "카테관리자2");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"게임"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("게임"));
    }

    @Test
    @DisplayName("일반 사용자는 카테고리를 만들 수 없다")
    void nonAdminCannotCreate() throws Exception {

        String token = signupAndLogin("cat-user@test.com", "일반유저");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"음악"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("이미 있는 이름으로는 만들 수 없다")
    void duplicateNameFails() throws Exception {

        String token = adminToken("cat-admin3@test.com", "카테관리자3");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"토크"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"토크"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("영상을 카테고리와 함께 등록하면 목록에서 카테고리로 필터링할 수 있다")
    void filterStreamsByCategory() throws Exception {

        String adminTok = adminToken("cat-admin4@test.com", "카테관리자4");

        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"버라이어티"}"""))
                .andExpect(status().isCreated())
                .andReturn();

        long categoryId = json(categoryResult).path("data").path("id").asLong();

        String userTok = signupAndLogin("cat-user2@test.com", "영상올리는이");

        mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + userTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"분류된영상","videoUrl":"/uploads/a.mp4","categoryId":%d}"""
                                .formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.categoryId").value(categoryId))
                .andExpect(jsonPath("$.data.categoryName").value("버라이어티"));

        mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + userTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"미분류영상","videoUrl":"/uploads/b.mp4"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/streams").param("categoryId", String.valueOf(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("분류된영상"));
    }

    @Test
    @DisplayName("존재하지 않는 카테고리로 영상을 등록할 수 없다")
    void createStreamWithUnknownCategoryFails() throws Exception {

        String token = signupAndLogin("cat-user3@test.com", "잘못된카테고리");

        mockMvc.perform(post("/api/streams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","videoUrl":"/uploads/a.mp4","categoryId":9999}"""))
                .andExpect(status().isNotFound());
    }
}
