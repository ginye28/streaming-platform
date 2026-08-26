package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUserIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("관리자는 사용자 목록을 조회할 수 있다")
    void adminCanListUsers() throws Exception {

        signup("listed@test.com", "목록에나올이");
        String adminTok = adminToken("useradmin1@test.com", "유저관리자1");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/admin/users")
                        .param("role", "ADMIN")
                        .header("Authorization", "Bearer " + adminTok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("useradmin1@test.com"));
    }

    @Test
    @DisplayName("일반 사용자는 사용자 목록을 볼 수 없다")
    void nonAdminCannotListUsers() throws Exception {

        String token = signupAndLogin("plain1@test.com", "일반1");

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자가 승격시킨 사용자는 관리자 기능을 쓸 수 있다")
    void promotedUserGainsAdminAbilities() throws Exception {

        String adminTok = adminToken("useradmin2@test.com", "유저관리자2");
        String targetTok = signupAndLogin("promote@test.com", "승격될이");
        long targetId = myUserId(targetTok);

        // 승격 전에는 카테고리를 만들 수 없다
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + targetTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"승격전"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/users/" + targetId + "/role")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        // 승격 후에는 가능하다
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + targetTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"승격후"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 권한을 다시 USER 로 되돌릴 수 있다")
    void adminCanDemoteAnotherAdmin() throws Exception {

        String adminTok = adminToken("useradmin3@test.com", "유저관리자3");
        String otherTok = signupAndLogin("demote@test.com", "강등될이");
        long otherId = myUserId(otherTok);

        mockMvc.perform(patch("/api/admin/users/" + otherId + "/role")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/users/" + otherId + "/role")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        // 강등된 뒤에는 관리자 기능을 쓸 수 없다
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + otherTok))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("자신의 관리자 권한은 해제할 수 없다")
    void adminCannotDemoteSelf() throws Exception {

        String adminTok = adminToken("useradmin4@test.com", "유저관리자4");
        long myId = myUserId(adminTok);

        mockMvc.perform(patch("/api/admin/users/" + myId + "/role")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 사용자의 권한은 바꿀 수 없다")
    void changeRoleOfUnknownUserFails() throws Exception {

        String adminTok = adminToken("useradmin5@test.com", "유저관리자5");

        mockMvc.perform(patch("/api/admin/users/9999/role")
                        .header("Authorization", "Bearer " + adminTok)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isNotFound());
    }
}
