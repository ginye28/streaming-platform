package com.sp.api.integration;

import com.sp.api.chat.service.ChatService;
import com.sp.api.common.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 멤버십 전용 방송과 채팅. 영상은 방송이 시작될 때의 공개 대상(audience)으로, 채팅은 방송 중에도 바꿀 수 있는
 * 채팅 대상(chatAudience)으로 막는다. 막힌 시청자에게는 재생 주소가 내려가지 않는다.
 */
class LiveAccessIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ChatService chatService;

    /** 주인이 방송을 열고 방송 id 를 돌려준다. */
    private long open(String ownerToken, long ownerId, String audience, String chatAudience) throws Exception {
        saveLiveSetting(ownerToken, audience, chatAudience, 0);
        startBroadcast(ownerToken);
        return currentLiveId(ownerId);
    }

    @Test
    @DisplayName("구독자 전용 방송은 구독하지 않은 사람에게 재생 주소를 주지 않는다")
    void subscribersOnlyHidesPlayUrl() throws Exception {

        String owner = signupAndLogin("owner@access.com", "방송주인");
        String stranger = signupAndLogin("stranger@access.com", "지나가는이");
        String fan = signupAndLogin("fan@access.com", "구독자");

        long ownerId = myUserId(owner);
        long liveId = open(owner, ownerId, "SUBSCRIBERS", "ALL");

        subscribe(fan, ownerId);

        // 비로그인
        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.audience").value("SUBSCRIBERS"))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.hlsUrl").doesNotExist());

        // 로그인했지만 구독하지 않음
        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + stranger))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.hlsUrl").doesNotExist());

        // 구독자
        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.locked").value(false))
                .andExpect(jsonPath("$.data.hlsUrl").exists());

        // 주인은 구독하지 않아도 볼 수 있다
        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.locked").value(false))
                .andExpect(jsonPath("$.data.hlsUrl").exists());
    }

    @Test
    @DisplayName("유료 전용 방송은 일반 구독자에게도 잠겨 있고, 유료로 올리면 열린다")
    void paidOnlyNeedsPaidTier() throws Exception {

        String owner = signupAndLogin("owner@paid.com", "유료방송주인");
        String fan = signupAndLogin("fan@paid.com", "일반구독자");

        long ownerId = myUserId(owner);
        long liveId = open(owner, ownerId, "PAID", "ALL");

        subscribe(fan, ownerId);

        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.hlsUrl").doesNotExist());

        // 결제가 꺼져 있는 테스트 환경에서는 유료 전환이 바로 된다.
        mockMvc.perform(put("/api/channels/" + ownerId + "/subscription")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tier\":\"PAID\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.locked").value(false))
                .andExpect(jsonPath("$.data.hlsUrl").exists());
    }

    @Test
    @DisplayName("방송 목록에도 잠금 표시가 붙고, 잠긴 방송의 재생 주소는 목록에서도 숨겨진다")
    void liveListShowsLockWithoutUrl() throws Exception {

        String owner = signupAndLogin("owner@list.com", "목록주인");
        String fan = signupAndLogin("fan@list.com", "목록구독자");

        long ownerId = myUserId(owner);
        open(owner, ownerId, "SUBSCRIBERS", "ALL");

        subscribe(fan, ownerId);

        mockMvc.perform(get("/api/lives"))
                .andExpect(jsonPath("$.data.content[0].locked").value(true))
                .andExpect(jsonPath("$.data.content[0].hlsUrl").doesNotExist());

        mockMvc.perform(get("/api/lives").header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.content[0].locked").value(false))
                .andExpect(jsonPath("$.data.content[0].hlsUrl").exists());
    }

    @Test
    @DisplayName("공개 방송은 누구나 볼 수 있다")
    void openLiveIsNotLocked() throws Exception {

        String owner = signupAndLogin("owner@open.com", "공개주인");

        long liveId = open(owner, myUserId(owner), "ALL", "ALL");

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.audience").value("ALL"))
                .andExpect(jsonPath("$.data.locked").value(false))
                .andExpect(jsonPath("$.data.hlsUrl").exists());
    }

    @Test
    @DisplayName("방송은 시작할 때마다 새 재생 이름을 받는다 (주소가 방송마다 다르다)")
    void eachBroadcastHasItsOwnPlayName() throws Exception {

        String owner = signupAndLogin("owner@name.com", "이름주인");

        String first = startBroadcast(owner);
        endBroadcast(first);
        String second = startBroadcast(owner);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("구독자 전용 방송의 채팅 내역은 구독하지 않은 사람에게 열리지 않는다")
    void lockedChatHistoryIsForbidden() throws Exception {

        String owner = signupAndLogin("owner@hist.com", "내역주인");
        String fan = signupAndLogin("fan@hist.com", "내역구독자");

        long ownerId = myUserId(owner);
        long liveId = open(owner, ownerId, "SUBSCRIBERS", "ALL");

        subscribe(fan, ownerId);

        mockMvc.perform(get("/api/lives/" + liveId + "/chats").header("Authorization", "Bearer " + fan))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("채팅 대상을 구독자로 정하면 구독자와 주인만 채팅할 수 있다")
    void chatAudienceLimitsSenders() throws Exception {

        String owner = signupAndLogin("owner@chataud.com", "채팅주인");
        String fan = signupAndLogin("fan@chataud.com", "채팅구독자");
        signupAndLogin("outsider@chataud.com", "채팅외부인");

        long ownerId = myUserId(owner);
        long liveId = open(owner, ownerId, "ALL", "SUBSCRIBERS");

        subscribe(fan, ownerId);

        assertThat(chatService.send(liveId, "fan@chataud.com", "구독자 채팅").content()).isEqualTo("구독자 채팅");
        assertThat(chatService.send(liveId, "owner@chataud.com", "주인 채팅").content()).isEqualTo("주인 채팅");

        assertThatThrownBy(() -> chatService.send(liveId, "outsider@chataud.com", "외부인 채팅"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("구독자만");
    }

    @Test
    @DisplayName("유료 전용 채팅은 일반 구독자도 막는다")
    void paidChatAudienceBlocksBasicSubscribers() throws Exception {

        String owner = signupAndLogin("owner@paidchat.com", "유료채팅주인");
        String fan = signupAndLogin("fan@paidchat.com", "일반팬");

        long ownerId = myUserId(owner);
        long liveId = open(owner, ownerId, "ALL", "PAID");

        subscribe(fan, ownerId);

        assertThatThrownBy(() -> chatService.send(liveId, "fan@paidchat.com", "일반 구독자 채팅"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("유료 구독자만");
    }

    @Test
    @DisplayName("주인은 방송 중에 채팅 대상을 바꿀 수 있고, 다른 사람은 바꿀 수 없다")
    void ownerChangesChatAudienceDuringBroadcast() throws Exception {

        String owner = signupAndLogin("owner@switch.com", "전환주인");
        String viewer = signupAndLogin("viewer@switch.com", "전환시청자");

        long liveId = open(owner, myUserId(owner), "ALL", "ALL");

        mockMvc.perform(put("/api/lives/" + liveId + "/chat-audience")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatAudience\":\"SUBSCRIBERS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.chatAudience").value("SUBSCRIBERS"));

        mockMvc.perform(put("/api/lives/" + liveId + "/chat-audience")
                        .header("Authorization", "Bearer " + viewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatAudience\":\"ALL\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("방송 하나를 조회하면 내 역할과 채팅 잠금 여부가 함께 온다")
    void detailCarriesMyRoleAndChatLock() throws Exception {

        String owner = signupAndLogin("owner@role.com", "역할주인");
        String fan = signupAndLogin("fan@role.com", "역할구독자");
        String stranger = signupAndLogin("stranger@role.com", "역할외부인");

        long ownerId = myUserId(owner);
        long liveId = open(owner, ownerId, "ALL", "SUBSCRIBERS");

        subscribe(fan, ownerId);

        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.myRole").value("OWNER"))
                .andExpect(jsonPath("$.data.chatLocked").value(false));

        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.myRole").doesNotExist())
                .andExpect(jsonPath("$.data.chatLocked").value(false));

        mockMvc.perform(get("/api/lives/" + liveId).header("Authorization", "Bearer " + stranger))
                .andExpect(jsonPath("$.data.chatLocked").value(true));

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.chatLocked").value(true));
    }

    @Test
    @DisplayName("방송 설정에 공개 대상·채팅 대상·슬로우 모드를 저장하고 다시 읽을 수 있다")
    void settingRoundTrip() throws Exception {

        String owner = signupAndLogin("owner@setting.com", "설정주인");

        saveLiveSetting(owner, "PAID", "SUBSCRIBERS", 10);

        mockMvc.perform(get("/api/lives/settings").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.audience").value("PAID"))
                .andExpect(jsonPath("$.data.chatAudience").value("SUBSCRIBERS"))
                .andExpect(jsonPath("$.data.slowModeSeconds").value(10));

        // 방송을 열면 그 설정이 방송에 적용된다
        startBroadcast(owner);

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/live"))
                .andExpect(jsonPath("$.data.audience").value("PAID"))
                .andExpect(jsonPath("$.data.chatAudience").value("SUBSCRIBERS"))
                .andExpect(jsonPath("$.data.slowModeSeconds").value(10));
    }

    @Test
    @DisplayName("슬로우 모드는 정해진 값 중에서만 고를 수 있다")
    void slowModeOnlyAllowsFixedChoices() throws Exception {

        String owner = signupAndLogin("owner@choice.com", "선택주인");

        mockMvc.perform(put("/api/lives/settings")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"방송\",\"slowModeSeconds\":7}"))
                .andExpect(status().isBadRequest());
    }
}
