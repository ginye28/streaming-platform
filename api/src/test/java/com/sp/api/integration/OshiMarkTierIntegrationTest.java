package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 오시마크 확장 — 유료 구독자용 마크, 구독 등급, 시청자가 고르는 마크 표시 여부.
 * 규칙은 한 곳(ChannelProfileService.oshiMarksFor)에 있으므로 댓글로 규칙을 확인하고,
 * 채팅은 같은 규칙을 쓰는지만 확인한다.
 */
class OshiMarkTierIntegrationTest extends IntegrationTestSupport {

    private static final String BASIC = "/uploads/basic.png";
    private static final String PAID = "/uploads/paid.png";

    @Autowired
    private com.sp.api.chat.service.ChatService chatService;

    // ---- 도우미 ----

    private void saveProfile(String ownerToken, String body) throws Exception {
        mockMvc.perform(put("/api/users/me/channel-profile")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions updateSubscription(
            String token, long channelId, String body
    ) throws Exception {
        return mockMvc.perform(put("/api/channels/" + channelId + "/subscription")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void comment(String token, long streamId, String content) throws Exception {
        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"%s"}""".formatted(content)))
                .andExpect(status().isCreated());
    }

    /** 이 채널 영상에 소유자가 아닌 사람이 단 첫 댓글(최신 댓글) 한 건의 마크를 본다. */
    private org.springframework.test.web.servlet.ResultActions latestComment(long streamId) throws Exception {
        return mockMvc.perform(get("/api/streams/" + streamId + "/comments"))
                .andExpect(status().isOk());
    }

    // ---- 규칙 ----

    @Test
    @DisplayName("일반 구독자는 일반 마크, 유료 구독자는 유료 마크를 단다")
    void paidSubscriberGetsPaidMark() throws Exception {

        String owner = signupAndLogin("oshi-owner1@test.com", "마크주인1");
        saveProfile(owner, """
                {"oshiMarkUrl":"%s","paidOshiMarkUrl":"%s"}""".formatted(BASIC, PAID));
        long channelId = myUserId(owner);
        long streamId = createStream(owner, "영상1");

        String basicFan = signupAndLogin("oshi-basic1@test.com", "일반팬");
        String paidFan = signupAndLogin("oshi-paid1@test.com", "유료팬");

        subscribe(basicFan, channelId);
        subscribe(paidFan, channelId);
        updateSubscription(paidFan, channelId, """
                {"tier":"PAID"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("PAID"));

        comment(basicFan, streamId, "일반 댓글");
        comment(paidFan, streamId, "유료 댓글");

        // 최신순이라 유료팬이 먼저 온다
        latestComment(streamId)
                .andExpect(jsonPath("$.data.content[0].nickname").value("유료팬"))
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").value(PAID))
                .andExpect(jsonPath("$.data.content[0].oshiTier").value("PAID"))
                .andExpect(jsonPath("$.data.content[1].nickname").value("일반팬"))
                .andExpect(jsonPath("$.data.content[1].oshiMarkUrl").value(BASIC))
                .andExpect(jsonPath("$.data.content[1].oshiTier").value("BASIC"));
    }

    @Test
    @DisplayName("유료 마크를 안 올린 채널이면 유료 구독자도 일반 마크를 단다")
    void paidFallsBackToBasicMark() throws Exception {

        String owner = signupAndLogin("oshi-owner2@test.com", "마크주인2");
        saveProfile(owner, """
                {"oshiMarkUrl":"%s"}""".formatted(BASIC));
        long channelId = myUserId(owner);
        long streamId = createStream(owner, "영상2");

        String paidFan = signupAndLogin("oshi-paid2@test.com", "유료팬2");
        subscribe(paidFan, channelId);
        updateSubscription(paidFan, channelId, """
                {"tier":"PAID"}""").andExpect(status().isOk());

        comment(paidFan, streamId, "댓글");

        latestComment(streamId)
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").value(BASIC))
                // 등급은 실제 구독 등급 그대로다
                .andExpect(jsonPath("$.data.content[0].oshiTier").value("PAID"));
    }

    @Test
    @DisplayName("구독자가 마크를 끄면 붙지 않고, 다시 켜면 붙는다")
    void subscriberCanHideAndShowMark() throws Exception {

        String owner = signupAndLogin("oshi-owner3@test.com", "마크주인3");
        saveProfile(owner, """
                {"oshiMarkUrl":"%s"}""".formatted(BASIC));
        long channelId = myUserId(owner);
        long streamId = createStream(owner, "영상3");

        String fan = signupAndLogin("oshi-hide3@test.com", "숨김팬");
        subscribe(fan, channelId);
        comment(fan, streamId, "댓글");

        latestComment(streamId)
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").value(BASIC));

        updateSubscription(fan, channelId, """
                {"markVisible":false}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markVisible").value(false))
                // 구독은 그대로다
                .andExpect(jsonPath("$.data.subscribed").value(true));

        latestComment(streamId)
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].oshiTier").doesNotExist());

        updateSubscription(fan, channelId, """
                {"markVisible":true}""").andExpect(status().isOk());

        latestComment(streamId)
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").value(BASIC));
    }

    @Test
    @DisplayName("구독하지 않은 사람에게는 마크가 붙지 않는다")
    void nonSubscriberGetsNoMark() throws Exception {

        String owner = signupAndLogin("oshi-owner4@test.com", "마크주인4");
        saveProfile(owner, """
                {"oshiMarkUrl":"%s","paidOshiMarkUrl":"%s"}""".formatted(BASIC, PAID));
        long streamId = createStream(owner, "영상4");

        String stranger = signupAndLogin("oshi-stranger4@test.com", "지나가던사람4");
        comment(stranger, streamId, "댓글");

        latestComment(streamId)
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").doesNotExist());
    }

    @Test
    @DisplayName("마크는 그 영상의 채널 기준이다 — 다른 채널 구독자는 이 영상에서 마크가 없다")
    void markBelongsToTheStreamsChannel() throws Exception {

        String ownerA = signupAndLogin("oshi-ownerA@test.com", "채널A");
        saveProfile(ownerA, """
                {"oshiMarkUrl":"%s"}""".formatted(BASIC));
        long channelA = myUserId(ownerA);

        String ownerB = signupAndLogin("oshi-ownerB@test.com", "채널B");
        long streamB = createStream(ownerB, "B의 영상");

        String fan = signupAndLogin("oshi-fanA@test.com", "A팬");
        subscribe(fan, channelA);
        comment(fan, streamB, "B 채널에 단 댓글");

        latestComment(streamB)
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").doesNotExist());
    }

    @Test
    @DisplayName("방금 쓴 댓글 응답에도 마크가 실린다")
    void createResponseCarriesMark() throws Exception {

        String owner = signupAndLogin("oshi-owner5@test.com", "마크주인5");
        saveProfile(owner, """
                {"oshiMarkUrl":"%s"}""".formatted(BASIC));
        long channelId = myUserId(owner);
        long streamId = createStream(owner, "영상5");

        String fan = signupAndLogin("oshi-fan5@test.com", "팬5");
        subscribe(fan, channelId);

        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"바로 보이나요"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.oshiMarkUrl").value(BASIC));
    }

    @Test
    @DisplayName("답글에도 같은 규칙으로 마크가 붙는다")
    void repliesCarryMarkToo() throws Exception {

        String owner = signupAndLogin("oshi-owner6@test.com", "마크주인6");
        saveProfile(owner, """
                {"oshiMarkUrl":"%s","paidOshiMarkUrl":"%s"}""".formatted(BASIC, PAID));
        long channelId = myUserId(owner);
        long streamId = createStream(owner, "영상6");

        String fan = signupAndLogin("oshi-fan6@test.com", "팬6");
        subscribe(fan, channelId);
        updateSubscription(fan, channelId, """
                {"tier":"PAID"}""").andExpect(status().isOk());

        comment(owner, streamId, "주인 댓글");
        long rootId = json(latestComment(streamId).andReturn())
                .path("data").path("content").get(0).path("id").asLong();

        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"답글","parentId":%d}""".formatted(rootId)))
                .andExpect(status().isCreated());

        latestComment(streamId)
                // 주인 본인은 구독자가 아니므로 마크가 없다
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].replies[0].oshiMarkUrl").value(PAID))
                .andExpect(jsonPath("$.data.content[0].replies[0].oshiTier").value("PAID"));
    }

    @Test
    @DisplayName("채팅도 같은 규칙으로 유료 마크를 단다")
    void chatUsesTheSameRule() throws Exception {

        String owner = signupAndLogin("oshi-owner7@test.com", "마크주인7");
        saveProfile(owner, """
                {"oshiMarkUrl":"%s","paidOshiMarkUrl":"%s"}""".formatted(BASIC, PAID));
        long channelId = myUserId(owner);
        String publicName = startBroadcast(owner);
        long liveId = json(mockMvc.perform(get("/api/channels/" + channelId + "/live"))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("id").asLong();

        String fan = signupAndLogin("oshi-fan7@test.com", "채팅팬");
        subscribe(fan, channelId);
        updateSubscription(fan, channelId, """
                {"tier":"PAID"}""").andExpect(status().isOk());

        chatService.send(liveId, "oshi-fan7@test.com", "안녕하세요");

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").value(PAID))
                .andExpect(jsonPath("$.data.content[0].oshiTier").value("PAID"));

        endBroadcast(publicName);
    }

    // ---- 설정 API ----

    @Test
    @DisplayName("구독 중이 아닌 채널의 설정은 바꿀 수 없다")
    void cannotUpdateWithoutSubscription() throws Exception {

        String owner = signupAndLogin("oshi-owner8@test.com", "마크주인8");
        String other = signupAndLogin("oshi-other8@test.com", "구독안함");

        updateSubscription(other, myUserId(owner), """
                {"tier":"PAID"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 없이는 설정을 바꿀 수 없다")
    void updateRequiresLogin() throws Exception {

        String owner = signupAndLogin("oshi-owner9@test.com", "마크주인9");

        mockMvc.perform(put("/api/channels/" + myUserId(owner) + "/subscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tier":"PAID"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("바꿀 값이 없거나 등급 이름이 틀리면 400")
    void rejectsEmptyOrUnknownValues() throws Exception {

        String owner = signupAndLogin("oshi-owner10@test.com", "마크주인10");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("oshi-fan10@test.com", "팬10");
        subscribe(fan, channelId);

        updateSubscription(fan, channelId, "{}").andExpect(status().isBadRequest());
        updateSubscription(fan, channelId, """
                {"tier":"GOLD"}""").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구독을 해제했다가 다시 하면 등급과 표시 설정이 처음으로 돌아간다")
    void resubscribeResetsSettings() throws Exception {

        String owner = signupAndLogin("oshi-owner11@test.com", "마크주인11");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("oshi-fan11@test.com", "팬11");

        subscribe(fan, channelId);
        updateSubscription(fan, channelId, """
                {"tier":"PAID","markVisible":false}""").andExpect(status().isOk());

        subscribe(fan, channelId); // 해제
        subscribe(fan, channelId); // 다시 구독

        mockMvc.perform(get("/api/channels/" + channelId)
                        .header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.subscribedByMe").value(true))
                .andExpect(jsonPath("$.data.myTier").value("BASIC"))
                .andExpect(jsonPath("$.data.myMarkVisible").value(true));
    }

    @Test
    @DisplayName("채널과 내 구독 목록이 내 등급과 표시 설정을 알려 준다")
    void channelAndSubscriptionListShowMySettings() throws Exception {

        String owner = signupAndLogin("oshi-owner12@test.com", "마크주인12");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("oshi-fan12@test.com", "팬12");

        // 구독 전에는 등급이 없다
        mockMvc.perform(get("/api/channels/" + channelId)
                        .header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.subscribedByMe").value(false))
                .andExpect(jsonPath("$.data.myTier").doesNotExist())
                .andExpect(jsonPath("$.data.myMarkVisible").value(false));

        subscribe(fan, channelId);
        updateSubscription(fan, channelId, """
                {"tier":"PAID","markVisible":false}""").andExpect(status().isOk());

        mockMvc.perform(get("/api/channels/" + channelId)
                        .header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.myTier").value("PAID"))
                .andExpect(jsonPath("$.data.myMarkVisible").value(false));

        mockMvc.perform(get("/api/users/me/subscriptions")
                        .header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.content[0].myTier").value("PAID"))
                .andExpect(jsonPath("$.data.content[0].myMarkVisible").value(false));
    }

    // ---- 채널 프로필 ----

    @Test
    @DisplayName("유료 오시마크를 저장하고, 비우면 지워진다")
    void savesAndClearsPaidMark() throws Exception {

        String owner = signupAndLogin("oshi-owner13@test.com", "마크주인13");
        long channelId = myUserId(owner);

        saveProfile(owner, """
                {"oshiMarkUrl":"%s","paidOshiMarkUrl":"%s"}""".formatted(BASIC, PAID));

        mockMvc.perform(get("/api/channels/" + channelId + "/profile"))
                .andExpect(jsonPath("$.data.oshiMarkUrl").value(BASIC))
                .andExpect(jsonPath("$.data.paidOshiMarkUrl").value(PAID));

        saveProfile(owner, """
                {"oshiMarkUrl":"%s","paidOshiMarkUrl":""}""".formatted(BASIC));

        mockMvc.perform(get("/api/channels/" + channelId + "/profile"))
                .andExpect(jsonPath("$.data.paidOshiMarkUrl").doesNotExist());
    }
}
