package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IntroGateIntegrationTest extends IntegrationTestSupport {

    /** 인트로를 저장한다. */
    private void saveIntro(String token, String headline) throws Exception {

        mockMvc.perform(put("/api/users/me/intro")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"headline":"%s","greeting":"처음 오셨군요."}""".formatted(headline)))
                .andExpect(status().isOk());
    }

    private long channelIdOf(String token) throws Exception {
        return myUserId(token);
    }

    @Test
    @DisplayName("인트로를 올린 채널에 처음 들어오면 인트로가 뜬다")
    void showsGateOnFirstVisit() throws Exception {

        String owner = signupAndLogin("intro-owner1@test.com", "인트로주인1");
        saveIntro(owner, "게임 방송 합니다");

        long channelId = channelIdOf(owner);

        mockMvc.perform(get("/api/channels/" + channelId + "/intro").with(remoteAddr("10.5.0.1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showGate").value(true))
                .andExpect(jsonPath("$.data.headline").value("게임 방송 합니다"))
                .andExpect(jsonPath("$.data.nickname").value("인트로주인1"));
    }

    @Test
    @DisplayName("인트로를 안 올린 채널은 그냥 들여보낸다")
    void noGateWithoutIntro() throws Exception {

        String owner = signupAndLogin("intro-owner2@test.com", "인트로주인2");

        mockMvc.perform(get("/api/channels/" + channelIdOf(owner) + "/intro")
                        .with(remoteAddr("10.5.0.2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showGate").value(false));
    }

    @Test
    @DisplayName("한 번 본 인트로는 다시 뜨지 않는다")
    void doesNotShowTwice() throws Exception {

        String owner = signupAndLogin("intro-owner3@test.com", "인트로주인3");
        saveIntro(owner, "두 번은 안 뜹니다");

        long channelId = channelIdOf(owner);

        mockMvc.perform(post("/api/channels/" + channelId + "/intro/seen")
                        .with(remoteAddr("10.5.0.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"SKIP"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/channels/" + channelId + "/intro").with(remoteAddr("10.5.0.3")))
                .andExpect(jsonPath("$.data.showGate").value(false));
    }

    @Test
    @DisplayName("비로그인 시청자도 접속 IP 로 기억한다 — 다른 IP 면 다시 뜬다")
    void remembersAnonymousViewerByIp() throws Exception {

        String owner = signupAndLogin("intro-owner4@test.com", "인트로주인4");
        saveIntro(owner, "아이피로 기억");

        long channelId = channelIdOf(owner);

        mockMvc.perform(post("/api/channels/" + channelId + "/intro/seen")
                        .with(remoteAddr("10.5.0.4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"PASS"}"""))
                .andExpect(status().isOk());

        // 같은 IP 는 다시 안 뜬다
        mockMvc.perform(get("/api/channels/" + channelId + "/intro").with(remoteAddr("10.5.0.4")))
                .andExpect(jsonPath("$.data.showGate").value(false));

        // 다른 IP 는 처음이므로 뜬다
        mockMvc.perform(get("/api/channels/" + channelId + "/intro").with(remoteAddr("10.5.0.99")))
                .andExpect(jsonPath("$.data.showGate").value(true));
    }

    @Test
    @DisplayName("구독 중인 채널은 인트로를 띄우지 않는다")
    void noGateForSubscribedChannel() throws Exception {

        String owner = signupAndLogin("intro-owner5@test.com", "인트로주인5");
        saveIntro(owner, "이미 아는 사람");

        long channelId = channelIdOf(owner);
        String viewer = signupAndLogin("intro-viewer5@test.com", "인트로시청자5");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/channels/" + channelId + "/intro")
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(jsonPath("$.data.showGate").value(false))
                .andExpect(jsonPath("$.data.subscribed").value(true));
    }

    @Test
    @DisplayName("자기 방송에는 인트로가 뜨지 않는다")
    void noGateForOwnChannel() throws Exception {

        String owner = signupAndLogin("intro-owner6@test.com", "인트로주인6");
        saveIntro(owner, "내 방송");

        mockMvc.perform(get("/api/channels/" + channelIdOf(owner) + "/intro")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.showGate").value(false));
    }

    @Test
    @DisplayName("빈 인트로를 저장하면 띄우지 않는다")
    void emptyIntroDoesNotShow() throws Exception {

        String owner = signupAndLogin("intro-owner7@test.com", "인트로주인7");

        mockMvc.perform(put("/api/users/me/intro")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"headline":"   ","greeting":""}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/channels/" + channelIdOf(owner) + "/intro")
                        .with(remoteAddr("10.5.0.7")))
                .andExpect(jsonPath("$.data.showGate").value(false));
    }

    @Test
    @DisplayName("한 줄 소개가 60자를 넘으면 400 이다")
    void rejectsTooLongHeadline() throws Exception {

        String owner = signupAndLogin("intro-owner8@test.com", "인트로주인8");

        mockMvc.perform(put("/api/users/me/intro")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"headline":"%s"}""".formatted("가".repeat(61))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이어보기는 이미 인트로를 본 채널을 건너뛴다")
    void nextSkipsSeenChannels() throws Exception {

        String first = signupAndLogin("next-owner1@test.com", "이어보기주인1");
        String second = signupAndLogin("next-owner2@test.com", "이어보기주인2");

        startBroadcast(first);
        startBroadcast(second);

        long firstChannel = channelIdOf(first);

        // 첫 번째 채널을 넘긴다
        mockMvc.perform(post("/api/channels/" + firstChannel + "/intro/seen")
                        .with(remoteAddr("10.6.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"PASS"}"""))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/lives/next").with(remoteAddr("10.6.0.1")))
                .andExpect(status().isOk())
                .andReturn();

        long nextChannelId = json(result).path("data").path("channelId").asLong();

        org.assertj.core.api.Assertions.assertThat(nextChannelId).isNotEqualTo(firstChannel);
    }

    @Test
    @DisplayName("더 볼 방송이 없으면 404 다")
    void nextReturnsNotFoundWhenNothingLeft() throws Exception {

        mockMvc.perform(get("/api/lives/next").with(remoteAddr("10.6.0.250")))
                .andExpect(status().isNotFound());
    }
}
