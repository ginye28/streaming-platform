package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LiveIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("유효하지 않은 스트림 키로는 송출할 수 없다")
    void publishWithUnknownKeyIsRejected() throws Exception {

        mockMvc.perform(post("/api/internal/rtmp/publish").param("name", "아무거나"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("스트림 키로 송출하면 공개 이름으로 리다이렉트된다 (키가 재생 URL 에 노출되지 않도록)")
    void publishRedirectsToPublicName() throws Exception {

        String token = signupAndLogin("pub@test.com", "송출자");
        String streamKey = streamKeyOf(token);

        var result = mockMvc.perform(post("/api/internal/rtmp/publish").param("name", streamKey))
                .andExpect(status().isFound())
                .andExpect(header().exists("Location"))
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        assertThat(location).startsWith("rtmp://");
        // 리다이렉트 주소에 송출 키가 들어가면 안 된다
        assertThat(location).doesNotContain(streamKey);
    }

    @Test
    @DisplayName("리다이렉트되어 공개 이름으로 다시 들어온 송출은 허용된다")
    void republishWithPublicNameIsAllowed() throws Exception {

        String token = signupAndLogin("republish@test.com", "재송출");
        String publicName = startBroadcast(token);

        mockMvc.perform(post("/api/internal/rtmp/publish").param("name", publicName))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("방송 중이 아닌 공개 이름으로는 송출할 수 없다")
    void republishWithoutActiveBroadcastIsRejected() throws Exception {

        String token = signupAndLogin("noactive@test.com", "비활성");
        String publicName = startBroadcast(token);

        endBroadcast(publicName);

        mockMvc.perform(post("/api/internal/rtmp/publish").param("name", publicName))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("송출을 시작하면 라이브 목록에 뜨고 종료하면 사라진다")
    void broadcastAppearsInLiveList() throws Exception {

        String token = signupAndLogin("livelist@test.com", "라이브목록");
        long channelId = myUserId(token);

        mockMvc.perform(get("/api/lives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        String publicName = startBroadcast(token);

        mockMvc.perform(get("/api/lives"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].nickname").value("라이브목록"))
                .andExpect(jsonPath("$.data.content[0].status").value("LIVE"))
                .andExpect(jsonPath("$.data.content[0].hlsUrl").value(
                        "http://localhost:8081/hls/" + publicName + ".m3u8"));

        // 채널 조회에도 방송 중 표시가 뜬다
        mockMvc.perform(get("/api/channels/" + channelId))
                .andExpect(jsonPath("$.data.live").value(true));

        endBroadcast(publicName);

        mockMvc.perform(get("/api/lives"))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/channels/" + channelId))
                .andExpect(jsonPath("$.data.live").value(false));
    }

    @Test
    @DisplayName("미리 저장한 방송 설정이 방송 제목으로 쓰인다")
    void liveSettingIsAppliedToBroadcast() throws Exception {

        String token = signupAndLogin("setting@test.com", "설정주인");

        mockMvc.perform(put("/api/lives/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"오늘의 방송","description":"설명입니다"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("오늘의 방송"));

        startBroadcast(token);

        mockMvc.perform(get("/api/lives"))
                .andExpect(jsonPath("$.data.content[0].title").value("오늘의 방송"))
                .andExpect(jsonPath("$.data.content[0].description").value("설명입니다"));
    }

    @Test
    @DisplayName("설정을 저장하지 않으면 기본 제목으로 방송이 열린다")
    void defaultBroadcastTitle() throws Exception {

        String token = signupAndLogin("notitle@test.com", "기본제목");

        startBroadcast(token);

        mockMvc.perform(get("/api/lives"))
                .andExpect(jsonPath("$.data.content[0].title").value("기본제목 님의 방송"));
    }

    @Test
    @DisplayName("방송 설정은 남이 볼 수 없다")
    void liveSettingRequiresAuth() throws Exception {

        mockMvc.perform(get("/api/lives/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("종료된 방송은 채널의 지난 방송 목록에 남는다")
    void endedBroadcastGoesToHistory() throws Exception {

        String token = signupAndLogin("history@test.com", "기록주인");
        long channelId = myUserId(token);

        String publicName = startBroadcast(token);
        endBroadcast(publicName);

        mockMvc.perform(get("/api/channels/" + channelId + "/live-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("ENDED"))
                .andExpect(jsonPath("$.data.content[0].endedAt").isNotEmpty());
    }

    @Test
    @DisplayName("방송 중이 아닌 채널의 라이브 조회는 404")
    void channelNotLive() throws Exception {

        String token = signupAndLogin("offline@test.com", "오프라인");
        long channelId = myUserId(token);

        mockMvc.perform(get("/api/channels/" + channelId + "/live"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("방송이 시작되면 구독자에게 알림이 간다")
    void subscribersAreNotifiedOnLiveStart() throws Exception {

        String ownerToken = signupAndLogin("notifyowner@test.com", "방송인");
        long channelId = myUserId(ownerToken);

        String subscriberToken = signupAndLogin("notifysub@test.com", "구독자");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        startBroadcast(ownerToken);

        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].type").value("LIVE_START"))
                .andExpect(jsonPath("$.data.content[0].message").value("방송인 님이 방송을 시작했습니다."))
                .andExpect(jsonPath("$.data.content[0].channelId").value(channelId))
                .andExpect(jsonPath("$.data.content[0].read").value(false));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    @DisplayName("구독하지 않은 사람에게는 알림이 가지 않는다")
    void nonSubscribersAreNotNotified() throws Exception {

        String ownerToken = signupAndLogin("quietowner@test.com", "조용한방송인");
        String strangerToken = signupAndLogin("stranger@test.com", "남남");

        startBroadcast(ownerToken);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    @DisplayName("알림을 모두 읽음 처리할 수 있다")
    void markAllNotificationsAsRead() throws Exception {

        String ownerToken = signupAndLogin("readowner@test.com", "읽음방송인");
        long channelId = myUserId(ownerToken);
        String subscriberToken = signupAndLogin("readsub@test.com", "읽음구독자");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(status().isOk());

        startBroadcast(ownerToken);

        mockMvc.perform(post("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(1));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + subscriberToken))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }
}
