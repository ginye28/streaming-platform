package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BlockIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("차단하면 다시 요청했을 때 해제된다 (토글)")
    void toggleBlock() throws Exception {

        String blockerToken = signupAndLogin("blocker1@test.com", "차단하는이1");
        long targetId = myUserId(signupAndLogin("target1@test.com", "차단대상1"));

        mockMvc.perform(post("/api/users/" + targetId + "/block")
                        .header("Authorization", "Bearer " + blockerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked").value(true));

        mockMvc.perform(post("/api/users/" + targetId + "/block")
                        .header("Authorization", "Bearer " + blockerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked").value(false));
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void cannotBlockSelf() throws Exception {

        String token = signupAndLogin("selfblock@test.com", "본인");
        long myId = myUserId(token);

        mockMvc.perform(post("/api/users/" + myId + "/block")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("차단한 채널의 영상은 전체 목록에서 보이지 않는다")
    void blockedUsersStreamsAreExcludedFromFeed() throws Exception {

        String blockerToken = signupAndLogin("blocker2@test.com", "차단하는이2");
        String targetToken = signupAndLogin("target2@test.com", "차단대상2");
        long targetId = myUserId(targetToken);

        createStream(targetToken, "차단대상의영상");
        createStream(blockerToken, "내영상");

        // 차단 전: 둘 다 보인다
        mockMvc.perform(get("/api/streams").header("Authorization", "Bearer " + blockerToken))
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(post("/api/users/" + targetId + "/block")
                        .header("Authorization", "Bearer " + blockerToken))
                .andExpect(status().isOk());

        // 차단 후: 차단한 채널의 영상만 빠진다
        mockMvc.perform(get("/api/streams").header("Authorization", "Bearer " + blockerToken))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("내영상"));

        // 비로그인이나 다른 사람에게는 여전히 둘 다 보인다
        mockMvc.perform(get("/api/streams"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @DisplayName("차단한 채널이 방송 중이어도 라이브 목록에서 보이지 않는다")
    void blockedUsersLivesAreExcludedFromFeed() throws Exception {

        String blockerToken = signupAndLogin("blocker3@test.com", "차단하는이3");
        String targetToken = signupAndLogin("target3@test.com", "차단대상3");
        long targetId = myUserId(targetToken);

        startBroadcast(targetToken);

        mockMvc.perform(get("/api/lives").header("Authorization", "Bearer " + blockerToken))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(post("/api/users/" + targetId + "/block")
                        .header("Authorization", "Bearer " + blockerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives").header("Authorization", "Bearer " + blockerToken))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("차단 목록을 조회할 수 있다")
    void listBlockedUsers() throws Exception {

        String blockerToken = signupAndLogin("blocker4@test.com", "차단하는이4");
        long targetId = myUserId(signupAndLogin("target4@test.com", "차단대상4"));

        mockMvc.perform(post("/api/users/" + targetId + "/block")
                        .header("Authorization", "Bearer " + blockerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/blocks")
                        .header("Authorization", "Bearer " + blockerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].nickname").value("차단대상4"));
    }
}
