package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChannelIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("채널 페이지는 비로그인도 볼 수 있고 구독자·영상 수를 함께 준다")
    void channelPageIsPublic() throws Exception {

        String token = signupAndLogin("ch1@test.com", "채널1");
        long channelId = myUserId(token);
        createStream(token, "영상1");
        createStream(token, "영상2");

        mockMvc.perform(get("/api/channels/" + channelId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("채널1"))
                .andExpect(jsonPath("$.data.streamCount").value(2))
                .andExpect(jsonPath("$.data.subscriberCount").value(0))
                .andExpect(jsonPath("$.data.subscribedByMe").value(false));
    }

    @Test
    @DisplayName("채널의 영상 목록을 조회할 수 있다")
    void channelStreams() throws Exception {

        String token = signupAndLogin("ch2@test.com", "채널2");
        long channelId = myUserId(token);
        createStream(token, "채널2의 영상");

        mockMvc.perform(get("/api/channels/" + channelId + "/streams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("채널2의 영상"))
                .andExpect(jsonPath("$.data.content[0].userId").value(channelId));
    }

    @Test
    @DisplayName("구독하면 채널 조회 시 subscribedByMe 가 true 가 된다")
    void subscribeReflectsInChannelPage() throws Exception {

        String ownerToken = signupAndLogin("owner3@test.com", "채널주인");
        long channelId = myUserId(ownerToken);

        String viewerToken = signupAndLogin("viewer3@test.com", "시청자");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscribed").value(true))
                .andExpect(jsonPath("$.data.subscriberCount").value(1));

        mockMvc.perform(get("/api/channels/" + channelId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscribedByMe").value(true));

        // 비로그인으로 보면 구독 여부는 false
        mockMvc.perform(get("/api/channels/" + channelId))
                .andExpect(jsonPath("$.data.subscribedByMe").value(false))
                .andExpect(jsonPath("$.data.subscriberCount").value(1));
    }

    @Test
    @DisplayName("자기 자신은 구독할 수 없다")
    void cannotSubscribeSelf() throws Exception {

        String token = signupAndLogin("self@test.com", "자기자신");
        long myId = myUserId(token);

        mockMvc.perform(post("/api/channels/" + myId + "/subscribe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구독 피드는 구독한 채널의 영상만 보여준다")
    void subscribedFeed() throws Exception {

        String ownerToken = signupAndLogin("feedowner@test.com", "피드주인");
        long channelId = myUserId(ownerToken);
        createStream(ownerToken, "구독한 채널의 영상");

        String otherToken = signupAndLogin("feedother@test.com", "남의채널");
        createStream(otherToken, "구독 안 한 채널의 영상");

        String viewerToken = signupAndLogin("feedviewer@test.com", "피드시청자");

        // 구독 전에는 비어 있다
        mockMvc.perform(get("/api/streams/subscribed")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/streams/subscribed")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("구독한 채널의 영상"));
    }

    @Test
    @DisplayName("구독 피드는 로그인이 필요하다")
    void subscribedFeedRequiresAuth() throws Exception {

        mockMvc.perform(get("/api/streams/subscribed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 구독 목록을 조회할 수 있다")
    void mySubscriptions() throws Exception {

        String ownerToken = signupAndLogin("subowner@test.com", "구독대상");
        long channelId = myUserId(ownerToken);

        String viewerToken = signupAndLogin("subviewer@test.com", "구독자");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/subscriptions")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].nickname").value("구독대상"))
                .andExpect(jsonPath("$.data.content[0].subscribedByMe").value(true));
    }

    @Test
    @DisplayName("없는 채널 조회는 404")
    void missingChannel() throws Exception {

        mockMvc.perform(get("/api/channels/999999"))
                .andExpect(status().isNotFound());
    }
}
