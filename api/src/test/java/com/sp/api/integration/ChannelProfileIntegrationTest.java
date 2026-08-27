package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChannelProfileIntegrationTest extends IntegrationTestSupport {

    @org.springframework.beans.factory.annotation.Autowired
    private com.sp.api.chat.service.ChatService chatService;

    private void saveProfile(String token, String body) throws Exception {

        mockMvc.perform(put("/api/users/me/channel-profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("아무것도 안 채운 채널도 같은 형태로 내려온다")
    void emptyProfileStillResponds() throws Exception {

        String owner = signupAndLogin("vt-empty@test.com", "빈프로필");

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("빈프로필"))
                .andExpect(jsonPath("$.data.fanName").doesNotExist())
                .andExpect(jsonPath("$.data.graduated").value(false))
                .andExpect(jsonPath("$.data.credits").isArray())
                .andExpect(jsonPath("$.data.credits").isEmpty());
    }

    @Test
    @DisplayName("오시마크 · 팬네임 · 데뷔일을 저장하고 공개로 조회한다")
    void savesAndExposesIdentity() throws Exception {

        String owner = signupAndLogin("vt-identity@test.com", "하늘별");

        saveProfile(owner, """
                {"oshiMarkUrl":"/uploads/mark.png","fanName":"별무리","debutOn":"2026-01-15"}""");

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(jsonPath("$.data.oshiMarkUrl").value("/uploads/mark.png"))
                .andExpect(jsonPath("$.data.fanName").value("별무리"))
                .andExpect(jsonPath("$.data.debutOn").value("2026-01-15"))
                .andExpect(jsonPath("$.data.daysUntilDebut").doesNotExist());
    }

    @Test
    @DisplayName("데뷔일이 아직 안 왔으면 남은 날수를 센다")
    void countsDaysUntilDebut() throws Exception {

        String owner = signupAndLogin("vt-debut@test.com", "데뷔예정");

        saveProfile(owner, """
                {"debutOn":"%s"}""".formatted(LocalDate.now().plusDays(7)));

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(jsonPath("$.data.daysUntilDebut").value(7));
    }

    @Test
    @DisplayName("졸업일을 넣으면 졸업으로 표시된다")
    void marksGraduated() throws Exception {

        String owner = signupAndLogin("vt-grad@test.com", "졸업생");

        saveProfile(owner, """
                {"graduatedOn":"%s"}""".formatted(LocalDate.now().minusDays(1)));

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(jsonPath("$.data.graduated").value(true));
    }

    @Test
    @DisplayName("졸업 예정일이 아직 안 왔으면 졸업이 아니다")
    void futureGraduationIsNotGraduatedYet() throws Exception {

        String owner = signupAndLogin("vt-grad2@test.com", "졸업예정");

        saveProfile(owner, """
                {"graduatedOn":"%s"}""".formatted(LocalDate.now().plusDays(10)));

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(jsonPath("$.data.graduated").value(false));
    }

    @Test
    @DisplayName("모델 크레딧은 넣은 차례대로 나온다")
    void keepsCreditOrder() throws Exception {

        String owner = signupAndLogin("vt-credit@test.com", "크레딧주인");

        saveProfile(owner, """
                {"credits":[
                  {"role":"ILLUSTRATOR","name":"그림작가","link":"https://x.com/a"},
                  {"role":"RIGGER","name":"리거"},
                  {"role":"BGM","name":"작곡가"}
                ]}""");

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(jsonPath("$.data.credits.length()").value(3))
                .andExpect(jsonPath("$.data.credits[0].role").value("ILLUSTRATOR"))
                .andExpect(jsonPath("$.data.credits[0].name").value("그림작가"))
                .andExpect(jsonPath("$.data.credits[1].role").value("RIGGER"))
                .andExpect(jsonPath("$.data.credits[1].link").doesNotExist())
                .andExpect(jsonPath("$.data.credits[2].name").value("작곡가"));
    }

    @Test
    @DisplayName("크레딧을 다시 보내면 통째로 갈아 끼운다")
    void replacesCredits() throws Exception {

        String owner = signupAndLogin("vt-credit2@test.com", "크레딧주인2");

        saveProfile(owner, """
                {"credits":[{"role":"ILLUSTRATOR","name":"처음작가"}]}""");
        saveProfile(owner, """
                {"credits":[{"role":"RIGGER","name":"바뀐리거"}]}""");

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(jsonPath("$.data.credits.length()").value(1))
                .andExpect(jsonPath("$.data.credits[0].name").value("바뀐리거"));
    }

    @Test
    @DisplayName("크레딧을 안 보내면 기존 것을 그대로 둔다")
    void keepsCreditsWhenOmitted() throws Exception {

        String owner = signupAndLogin("vt-credit3@test.com", "크레딧주인3");

        saveProfile(owner, """
                {"credits":[{"role":"ILLUSTRATOR","name":"남아야함"}]}""");
        saveProfile(owner, """
                {"fanName":"팬네임만바꿈"}""");

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/profile"))
                .andExpect(jsonPath("$.data.fanName").value("팬네임만바꿈"))
                .andExpect(jsonPath("$.data.credits.length()").value(1))
                .andExpect(jsonPath("$.data.credits[0].name").value("남아야함"));
    }

    @Test
    @DisplayName("이름 없는 크레딧은 400 이다")
    void rejectsCreditWithoutName() throws Exception {

        String owner = signupAndLogin("vt-credit4@test.com", "크레딧주인4");

        mockMvc.perform(put("/api/users/me/channel-profile")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credits":[{"role":"RIGGER","name":"  "}]}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구독한 사람의 채팅에는 오시마크가 붙는다")
    void attachesOshiMarkToSubscriberChat() throws Exception {

        String owner = signupAndLogin("vt-chat-owner@test.com", "채팅주인");
        saveProfile(owner, """
                {"oshiMarkUrl":"/uploads/star.png"}""");

        long channelId = myUserId(owner);
        String publicName = startBroadcast(owner);
        long liveId = liveIdOf(owner);

        String fan = signupAndLogin("vt-fan@test.com", "열혈팬");
        String stranger = signupAndLogin("vt-stranger@test.com", "지나가던사람");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + fan))
                .andExpect(status().isOk());

        sendChat(liveId, "vt-fan@test.com", "오시 최고");
        sendChat(liveId, "vt-stranger@test.com", "처음 왔어요");

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(status().isOk())
                // 최신순이라 지나가던 사람이 먼저 온다
                .andExpect(jsonPath("$.data.content[0].nickname").value("지나가던사람"))
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").doesNotExist())
                .andExpect(jsonPath("$.data.content[1].nickname").value("열혈팬"))
                .andExpect(jsonPath("$.data.content[1].oshiMarkUrl").value("/uploads/star.png"));

        endBroadcast(publicName);
    }

    @Test
    @DisplayName("오시마크를 안 올린 채널은 구독자에게도 안 붙는다")
    void noMarkWhenChannelHasNone() throws Exception {

        String owner = signupAndLogin("vt-nomark@test.com", "마크없음");

        long channelId = myUserId(owner);
        String publicName = startBroadcast(owner);
        long liveId = liveIdOf(owner);

        String fan = signupAndLogin("vt-nomark-fan@test.com", "마크없는팬");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + fan))
                .andExpect(status().isOk());

        sendChat(liveId, "vt-nomark-fan@test.com", "안녕하세요");

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").doesNotExist());

        endBroadcast(publicName);
    }

    /** 방송 중인 내 방송 id. */
    private long liveIdOf(String token) throws Exception {
        return json(mockMvc.perform(get("/api/channels/" + myUserId(token) + "/live"))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("id").asLong();
    }

    /** 채팅은 REST 가 아니라 WebSocket 으로만 보내므로 서비스를 직접 부른다. */
    private void sendChat(long liveId, String email, String content) {
        chatService.send(liveId, email, content);
    }
}
