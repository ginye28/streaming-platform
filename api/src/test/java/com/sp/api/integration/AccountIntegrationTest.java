package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("프로필(닉네임·이미지)을 수정할 수 있다")
    void updateProfile() throws Exception {

        String token = signupAndLogin("profile@test.com", "이전닉네임");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"새닉네임","profileImage":"/uploads/p.png"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.data.profileImage").value("/uploads/p.png"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("이미 쓰는 닉네임으로는 바꿀 수 없다")
    void updateProfileWithTakenNickname() throws Exception {

        signup("taken@test.com", "선점된닉네임");
        String token = signupAndLogin("changer@test.com", "바꿀사람");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"선점된닉네임"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("닉네임을 그대로 두고 프로필 이미지만 바꿔도 중복 오류가 나지 않는다")
    void updateProfileKeepingOwnNickname() throws Exception {

        String token = signupAndLogin("keep@test.com", "그대로");

        mockMvc.perform(patch("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"그대로","profileImage":"/uploads/new.png"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileImage").value("/uploads/new.png"));
    }

    @Test
    @DisplayName("비밀번호를 바꾸면 새 비밀번호로 로그인된다")
    void changePassword() throws Exception {

        String token = signupAndLogin("pw@test.com", "비번변경");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"newpassword456"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pw@test.com","password":"newpassword456"}"""))
                .andExpect(status().isOk());

        // 기존 비밀번호로는 더 이상 로그인되지 않는다
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pw@test.com","password":"password123"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 변경되지 않는다")
    void changePasswordWithWrongCurrent() throws Exception {

        String token = signupAndLogin("pw2@test.com", "비번변경2");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrongpassword","newPassword":"newpassword456"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("기존과 같은 비밀번호로는 변경할 수 없다")
    void changePasswordToSame() throws Exception {

        String token = signupAndLogin("pw3@test.com", "비번변경3");

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"password123"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("스트림 키는 재발급하면 값이 바뀐다")
    void regenerateStreamKey() throws Exception {

        String token = signupAndLogin("key@test.com", "키주인");

        String before = json(mockMvc.perform(get("/api/users/stream-key")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn())
                .path("data").path("streamKey").asString();

        String after = json(mockMvc.perform(post("/api/users/stream-key/regenerate")
                        .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn())
                .path("data").path("streamKey").asString();

        org.assertj.core.api.Assertions.assertThat(after).isNotBlank().isNotEqualTo(before);
    }
}
