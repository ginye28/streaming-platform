package com.sp.api.integration;

import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.chat.event.ChatBroadcast;
import com.sp.api.chat.event.ChatEvent;
import com.sp.api.chat.service.ChatService;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 채팅 운영: 매니저, 메시지 삭제·고정, 슬로우 모드, 일시 정지·강퇴, 금칙어. */
@RecordApplicationEvents
class ChatModerationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ApplicationEvents events;

    private String owner;
    private String manager;
    private String viewer;
    private String other;
    private long ownerId;
    private long managerId;
    private long viewerId;
    private long liveId;

    @BeforeEach
    void setUp() throws Exception {

        owner = signupAndLogin("owner@mod.com", "운영주인");
        manager = signupAndLogin("manager@mod.com", "운영매니저");
        viewer = signupAndLogin("viewer@mod.com", "운영시청자");
        other = signupAndLogin("other@mod.com", "운영다른이");

        ownerId = myUserId(owner);
        managerId = myUserId(manager);
        viewerId = myUserId(viewer);

        mockMvc.perform(post("/api/users/me/moderators/" + managerId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isCreated());

        startBroadcast(owner);
        liveId = currentLiveId(ownerId);
    }

    private ChatMessageResponse say(String email, String text) {
        return chatService.send(liveId, email, text);
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String path, String token, String body)
            throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions putJson(String path, String token, String body)
            throws Exception {
        return mockMvc.perform(put(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // ---- 매니저 ----

    @Test
    @DisplayName("주인은 매니저를 지정하고 목록을 보고 해제할 수 있다")
    void ownerManagesModerators() throws Exception {

        mockMvc.perform(get("/api/users/me/moderators").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(managerId))
                .andExpect(jsonPath("$.data[0].nickname").value("운영매니저"));

        // 같은 사람을 다시 지정해도 오류가 아니다
        mockMvc.perform(post("/api/users/me/moderators/" + managerId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/me/moderators").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(delete("/api/users/me/moderators/" + managerId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/moderators").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("자기 자신은 매니저로 지정할 수 없다")
    void cannotAppointSelf() throws Exception {

        mockMvc.perform(post("/api/users/me/moderators/" + ownerId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("채팅 메시지에는 쓴 사람의 역할(주인·매니저)이 붙는다")
    void messagesCarryRole() {

        assertThat(say("owner@mod.com", "주인").role()).isEqualTo("OWNER");
        assertThat(say("manager@mod.com", "매니저").role()).isEqualTo("MANAGER");
        assertThat(say("viewer@mod.com", "시청자").role()).isNull();
    }

    // ---- 메시지 삭제 · 고정 ----

    @Test
    @DisplayName("매니저는 남의 메시지를 지울 수 있고, 지운 메시지는 내용 없이 deleted 로만 보인다")
    void managerDeletesMessage() throws Exception {

        long messageId = say("viewer@mod.com", "지워질 말").id();

        mockMvc.perform(delete("/api/lives/" + liveId + "/chats/" + messageId)
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.content[0].deleted").value(true))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());

        assertThat(events.stream(ChatBroadcast.class)
                .filter(broadcast -> broadcast.payload() instanceof ChatEvent event
                        && "DELETE".equals(event.type()) && event.messageId() == messageId))
                .hasSize(1);
    }

    @Test
    @DisplayName("쓴 사람 본인은 자기 메시지를 지울 수 있다")
    void authorDeletesOwnMessage() throws Exception {

        long messageId = say("viewer@mod.com", "내 말").id();

        mockMvc.perform(delete("/api/lives/" + liveId + "/chats/" + messageId)
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 시청자는 남의 메시지를 지울 수 없다")
    void viewerCannotDeleteOthers() throws Exception {

        long messageId = say("owner@mod.com", "주인의 말").id();

        mockMvc.perform(delete("/api/lives/" + liveId + "/chats/" + messageId)
                        .header("Authorization", "Bearer " + viewer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("메시지를 고정하면 방송 조회에 나오고, 고정을 풀 수 있다")
    void pinAndUnpin() throws Exception {

        long messageId = say("viewer@mod.com", "공지할 말").id();

        putJson("/api/lives/" + liveId + "/pin", manager, "{\"messageId\":" + messageId + "}")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.pinnedMessage.id").value(messageId))
                .andExpect(jsonPath("$.data.pinnedMessage.content").value("공지할 말"));

        mockMvc.perform(delete("/api/lives/" + liveId + "/pin").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.pinnedMessage").doesNotExist());
    }

    @Test
    @DisplayName("고정한 메시지를 지우면 고정도 풀린다")
    void deletingPinnedMessageUnpins() throws Exception {

        long messageId = say("viewer@mod.com", "곧 지워질 공지").id();

        putJson("/api/lives/" + liveId + "/pin", owner, "{\"messageId\":" + messageId + "}")
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/lives/" + liveId + "/chats/" + messageId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.pinnedMessage").doesNotExist());
    }

    @Test
    @DisplayName("일반 시청자는 메시지를 고정할 수 없다")
    void viewerCannotPin() throws Exception {

        long messageId = say("viewer@mod.com", "고정하고 싶은 말").id();

        putJson("/api/lives/" + liveId + "/pin", viewer, "{\"messageId\":" + messageId + "}")
                .andExpect(status().isForbidden());
    }

    // ---- 슬로우 모드 ----

    @Test
    @DisplayName("슬로우 모드에서는 시청자가 바로 다시 보낼 수 없고, 주인과 매니저는 영향이 없다")
    void slowMode() throws Exception {

        putJson("/api/lives/" + liveId + "/slow-mode", manager, "{\"seconds\":10}").andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.slowModeSeconds").value(10));

        say("viewer@mod.com", "첫 번째");

        // 주인·매니저는 연달아 보낼 수 있다
        say("owner@mod.com", "하나");
        say("owner@mod.com", "둘");
        say("manager@mod.com", "하나");
        say("manager@mod.com", "둘");

        assertThatThrownBy(() -> say("viewer@mod.com", "두 번째"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("슬로우 모드");
    }

    @Test
    @DisplayName("슬로우 모드는 정해진 값만 고를 수 있다")
    void slowModeValidatesChoices() throws Exception {

        putJson("/api/lives/" + liveId + "/slow-mode", owner, "{\"seconds\":7}")
                .andExpect(status().isBadRequest());
    }

    // ---- 일시 정지 · 강퇴 ----

    @Test
    @DisplayName("일시 정지된 사람은 채팅할 수 없고, 풀면 다시 쓸 수 있다")
    void timeoutAndLift() throws Exception {

        postJson("/api/lives/" + liveId + "/restrictions", manager,
                "{\"userId\":" + viewerId + ",\"minutes\":10,\"reason\":\"도배\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permanent").value(false));

        mockMvc.perform(get("/api/users/me/chat-restrictions").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].reason").value("도배"));

        mockMvc.perform(delete("/api/lives/" + liveId + "/restrictions/" + viewerId)
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isOk());

        assertThat(say("viewer@mod.com", "풀린 뒤").content()).isEqualTo("풀린 뒤");
    }

    @Test
    @DisplayName("일시 정지 중에는 채팅이 거절되고 남은 시간을 알려 준다")
    void timedOutUserIsRejected() throws Exception {

        postJson("/api/lives/" + liveId + "/restrictions", owner,
                "{\"userId\":" + viewerId + ",\"minutes\":10}")
                .andExpect(status().isOk());

        assertThatThrownBy(() -> say("viewer@mod.com", "말하고 싶다"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("일시 정지")
                .hasMessageContaining("분");
    }

    @Test
    @DisplayName("강퇴된 사람은 풀어 줄 때까지 채팅할 수 없다")
    void bannedUserIsRejected() throws Exception {

        postJson("/api/lives/" + liveId + "/restrictions", owner, "{\"userId\":" + viewerId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permanent").value(true));

        assertThatThrownBy(() -> say("viewer@mod.com", "말하고 싶다"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("차단");
    }

    @Test
    @DisplayName("제한하면서 purge 를 켜면 그 사람이 쓴 메시지도 모두 지워진다")
    void restrictWithPurge() throws Exception {

        say("viewer@mod.com", "하나");
        say("viewer@mod.com", "둘");
        say("other@mod.com", "남아야 하는 말");

        postJson("/api/lives/" + liveId + "/restrictions", owner,
                "{\"userId\":" + viewerId + ",\"purge\":true}")
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.content[?(@.nickname=='운영시청자')].deleted")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(true))))
                .andExpect(jsonPath("$.data.content[?(@.nickname=='운영다른이')].deleted")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(false))));

        assertThat(events.stream(ChatBroadcast.class)
                .filter(broadcast -> broadcast.payload() instanceof ChatEvent event
                        && "PURGE".equals(event.type()) && event.userId() == viewerId))
                .hasSize(1);
    }

    @Test
    @DisplayName("매니저는 주인을 제한할 수 없다")
    void managerCannotRestrictOwner() throws Exception {

        postJson("/api/lives/" + liveId + "/restrictions", manager, "{\"userId\":" + ownerId + "}")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("주인은 매니저를 제한할 수 있지만 매니저끼리는 안 된다")
    void onlyOwnerCanRestrictManagers() throws Exception {

        postJson("/api/lives/" + liveId + "/restrictions", owner, "{\"userId\":" + managerId + ",\"minutes\":5}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 시청자는 사람을 제한할 수 없다")
    void viewerCannotRestrict() throws Exception {

        postJson("/api/lives/" + liveId + "/restrictions", viewer, "{\"userId\":" + myUserId(other) + "}")
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("제한은 채널에 걸려서 다음 방송에도 이어진다")
    void restrictionCarriesToNextBroadcast() throws Exception {

        postJson("/api/lives/" + liveId + "/restrictions", owner, "{\"userId\":" + viewerId + "}")
                .andExpect(status().isOk());

        endBroadcast(startedName());
        startBroadcast(owner);
        liveId = currentLiveId(ownerId);

        assertThatThrownBy(() -> say("viewer@mod.com", "새 방송에서도 막혀야 함"))
                .isInstanceOf(ForbiddenException.class);
    }

    /** 지금 방송의 재생 이름. */
    private String startedName() throws Exception {
        String url = json(mockMvc.perform(get("/api/lives/" + liveId)).andReturn())
                .path("data").path("hlsUrl").asString();
        return url.substring(url.lastIndexOf('/') + 1).replace(".m3u8", "");
    }

    // ---- 금칙어 ----

    @Test
    @DisplayName("금칙어가 든 채팅은 거절되고(대소문자·공백 무시), 주인은 영향이 없다")
    void bannedWords() throws Exception {

        postJson("/api/users/me/banned-words", owner, "{\"word\":\"Bad Word\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.word").value("badword"));

        mockMvc.perform(get("/api/users/me/banned-words").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.length()").value(1));

        // 주인과 매니저는 걸리지 않는다
        say("owner@mod.com", "주인은 BADWORD 도 쓴다");
        say("manager@mod.com", "매니저도 bad word 를 쓴다");

        assertThatThrownBy(() -> say("viewer@mod.com", "this is a B A D w o r d!"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("사용할 수 없는 단어");
    }

    @Test
    @DisplayName("같은 금칙어는 두 번 등록할 수 없고, 지우면 다시 쓸 수 있다")
    void bannedWordDuplicateAndRemove() throws Exception {

        postJson("/api/users/me/banned-words", owner, "{\"word\":\"금지어\"}").andExpect(status().isCreated());

        long wordId = json(mockMvc.perform(get("/api/users/me/banned-words")
                .header("Authorization", "Bearer " + owner)).andReturn()).path("data").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/users/me/banned-words/" + wordId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        assertThat(say("viewer@mod.com", "금지어 사용").content()).isEqualTo("금지어 사용");
    }

    @Test
    @DisplayName("이미 있는 금칙어를 다시 등록하면 409")
    void bannedWordConflict() throws Exception {

        postJson("/api/users/me/banned-words", owner, "{\"word\":\"중복\"}").andExpect(status().isCreated());

        postJson("/api/users/me/banned-words", owner, "{\"word\":\" 중 복 \"}").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("채팅이 막힌 사람 목록은 주인 본인만 읽는다 (로그인하지 않으면 401)")
    void restrictionListRequiresLogin() throws Exception {

        mockMvc.perform(get("/api/users/me/chat-restrictions")).andExpect(status().isUnauthorized());
    }
}
