package com.sp.api.integration;

import com.sp.api.chat.entity.ChatMessage;
import com.sp.api.chat.repository.ChatMessageRepository;
import com.sp.api.chat.service.ChatService;
import com.sp.api.live.entity.LiveStream;
import com.sp.api.live.repository.LiveStreamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 다시보기(녹화가 켜진 스트리밍 서버)와 다시보기 채팅. */
@TestPropertySource(properties = {
        "app.vod.enabled=true",
        "app.hls.base-url=http://localhost:8081/hls"
})
class LiveReplayIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private LiveStreamRepository liveStreamRepository;

    /** 방송을 열고 채팅을 남긴 뒤 끝낸다. 채팅의 시각을 방송 시작 기준으로 맞춰서 돌려준다. */
    private long endedLiveWithChats(String ownerToken, long ownerId, String[] texts, String sender) throws Exception {

        String name = startBroadcast(ownerToken);
        long liveId = currentLiveId(ownerId);

        for (String text : texts) {
            chatService.send(liveId, sender, text);
        }

        endBroadcast(name);

        return liveId;
    }

    @Test
    @DisplayName("다시보기가 켜져 있으면 끝난 방송이 다시보기 주소를 갖는다")
    void endedLiveHasVodUrl() throws Exception {

        String owner = signupAndLogin("owner@vod.com", "다시보기주인");

        String name = startBroadcast(owner);
        long liveId = currentLiveId(myUserId(owner));

        // 방송 중에는 다시보기가 없다
        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.vodUrl").doesNotExist());

        endBroadcast(name);

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.status").value("ENDED"))
                .andExpect(jsonPath("$.data.vodUrl").value("http://localhost:8081/vod/" + name + ".m3u8"));
    }

    @Test
    @DisplayName("지난 방송 기록에도 다시보기 주소가 실린다")
    void historyCarriesVodUrl() throws Exception {

        String owner = signupAndLogin("owner@history.com", "기록주인");
        long ownerId = myUserId(owner);

        String name = startBroadcast(owner);
        endBroadcast(name);

        mockMvc.perform(get("/api/channels/" + ownerId + "/live-history"))
                .andExpect(jsonPath("$.data.content[0].vodUrl").value("http://localhost:8081/vod/" + name + ".m3u8"));
    }

    @Test
    @DisplayName("구독자 전용 방송의 다시보기 주소는 구독하지 않은 사람에게 내려가지 않는다")
    void lockedVodIsHidden() throws Exception {

        String owner = signupAndLogin("owner@lockvod.com", "잠금다시보기주인");
        String fan = signupAndLogin("fan@lockvod.com", "잠금다시보기구독자");
        long ownerId = myUserId(owner);

        saveLiveSetting(owner, "SUBSCRIBERS", "ALL", 0);
        subscribe(fan, ownerId);

        String name = startBroadcast(owner);
        long liveId = currentLiveId(ownerId);
        endBroadcast(name);

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.vodUrl").doesNotExist());

        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.locked").value(false))
                .andExpect(jsonPath("$.data.vodUrl").exists());
    }

    // ---- 다시보기 채팅 ----

    @Test
    @DisplayName("다시보기 채팅은 오래된 순으로 나오고 방송 시작 뒤 몇 초째인지가 붙는다")
    void replayIsOrderedWithOffsets() throws Exception {

        String owner = signupAndLogin("owner@replay.com", "리플레이주인");
        signupAndLogin("viewer@replay.com", "리플레이시청자");
        long ownerId = myUserId(owner);

        long liveId = endedLiveWithChats(owner, ownerId, new String[]{"첫째", "둘째", "셋째"}, "viewer@replay.com");

        // 채팅 시각을 방송 시작 기준 10초·65초·3700초 뒤로 맞춘다
        LiveStream live = liveStreamRepository.findById(liveId).orElseThrow();
        LocalDateTime start = live.getStartedAt();
        long[] offsets = {10, 65, 3700};
        int index = 0;

        for (ChatMessage message : chatMessageRepository.findByLiveStreamIdOrderByIdDesc(
                liveId, org.springframework.data.domain.Pageable.unpaged()).getContent().reversed()) {
            ReflectionTestUtils.setField(message, "createdAt", start.plusSeconds(offsets[index++]));
        }

        chatMessageRepository.flush();

        mockMvc.perform(get("/api/lives/" + liveId + "/chats/replay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(3))
                .andExpect(jsonPath("$.data.messages[0].content").value("첫째"))
                .andExpect(jsonPath("$.data.messages[0].offsetSeconds").value(10))
                .andExpect(jsonPath("$.data.messages[1].offsetSeconds").value(65))
                .andExpect(jsonPath("$.data.messages[2].content").value("셋째"))
                .andExpect(jsonPath("$.data.messages[2].offsetSeconds").value(3700))
                .andExpect(jsonPath("$.data.nextAfterId").doesNotExist());
    }

    @Test
    @DisplayName("다시보기 채팅은 afterId 로 이어서 받는다")
    void replayPagesWithCursor() throws Exception {

        String owner = signupAndLogin("owner@cursor.com", "커서주인");
        signupAndLogin("viewer@cursor.com", "커서시청자");

        long liveId = endedLiveWithChats(owner, myUserId(owner),
                new String[]{"a", "b", "c", "d", "e"}, "viewer@cursor.com");

        var first = json(mockMvc.perform(get("/api/lives/" + liveId + "/chats/replay?size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(2))
                .andReturn()).path("data");

        long next = first.path("nextAfterId").asLong();

        mockMvc.perform(get("/api/lives/" + liveId + "/chats/replay?size=2&afterId=" + next))
                .andExpect(jsonPath("$.data.messages[0].content").value("c"))
                .andExpect(jsonPath("$.data.messages.length()").value(2));
    }

    @Test
    @DisplayName("지운 메시지는 다시보기 채팅에서 빠진다")
    void replaySkipsDeletedMessages() throws Exception {

        String owner = signupAndLogin("owner@skip.com", "삭제주인");
        signupAndLogin("viewer@skip.com", "삭제시청자");
        long ownerId = myUserId(owner);

        String name = startBroadcast(owner);
        long liveId = currentLiveId(ownerId);

        long removed = chatService.send(liveId, "viewer@skip.com", "지울 말").id();
        chatService.send(liveId, "viewer@skip.com", "남길 말");

        mockMvc.perform(delete("/api/lives/" + liveId + "/chats/" + removed)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        endBroadcast(name);

        mockMvc.perform(get("/api/lives/" + liveId + "/chats/replay"))
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.messages[0].content").value("남길 말"));
    }

    @Test
    @DisplayName("방송 중에는 다시보기 채팅을 받을 수 없다")
    void replayNeedsEndedLive() throws Exception {

        String owner = signupAndLogin("owner@during.com", "방송중주인");

        startBroadcast(owner);
        long liveId = currentLiveId(myUserId(owner));

        mockMvc.perform(get("/api/lives/" + liveId + "/chats/replay")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("구독자 전용 방송의 다시보기 채팅은 구독하지 않은 사람에게 닫혀 있다")
    void lockedReplayIsForbidden() throws Exception {

        String owner = signupAndLogin("owner@lockreplay.com", "잠금리플레이주인");
        long ownerId = myUserId(owner);

        saveLiveSetting(owner, "SUBSCRIBERS", "ALL", 0);

        String name = startBroadcast(owner);
        long liveId = currentLiveId(ownerId);
        endBroadcast(name);

        mockMvc.perform(get("/api/lives/" + liveId + "/chats/replay")).andExpect(status().isForbidden());
    }
}
