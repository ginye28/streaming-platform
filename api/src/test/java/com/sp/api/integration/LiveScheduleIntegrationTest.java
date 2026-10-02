package com.sp.api.integration;

import com.sp.api.live.entity.Audience;
import com.sp.api.live.schedule.LiveSchedule;
import com.sp.api.live.schedule.LiveScheduleRepository;
import com.sp.api.live.schedule.LiveScheduleService;
import com.sp.api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 방송 예약: 만들기·고치기·취소, 구독자 알림, 곧 시작 알림, 방송이 시작될 때의 이어 붙이기. */
class LiveScheduleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private LiveScheduleService scheduleService;

    @Autowired
    private LiveScheduleRepository scheduleRepository;

    /** 서버가 받는 모양(시간대가 붙은 시각)으로 바꾼다. */
    private static String at(LocalDateTime time) {
        return time.withNano(0).atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime()
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private ResultActions create(String token, String title, LocalDateTime when, String extra) throws Exception {
        return mockMvc.perform(post("/api/lives/schedules")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"%s\",\"scheduledAt\":\"%s\"%s}".formatted(title, at(when), extra)));
    }

    private long createId(String token, String title, LocalDateTime when) throws Exception {
        return json(create(token, title, when, "").andExpect(status().isCreated()).andReturn())
                .path("data").path("id").asLong();
    }

    // ---- 만들기 ----

    @Test
    @DisplayName("방송을 예약하면 누구나 대기실 정보를 읽을 수 있다")
    void createAndRead() throws Exception {

        String owner = signupAndLogin("owner@sch.com", "예약주인");
        LocalDateTime when = LocalDateTime.now().plusDays(1);

        long id = json(create(owner, "내일의 방송", when, ",\"description\":\"설명\",\"audience\":\"SUBSCRIBERS\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn()).path("data").path("id").asLong();

        mockMvc.perform(get("/api/lives/schedules/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("내일의 방송"))
                .andExpect(jsonPath("$.data.nickname").value("예약주인"))
                .andExpect(jsonPath("$.data.audience").value("SUBSCRIBERS"))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.liveId").doesNotExist());
    }

    @Test
    @DisplayName("방송 시각은 지금보다 뒤이고 90일 안이어야 한다")
    void timeMustBeInRange() throws Exception {

        String owner = signupAndLogin("owner@range.com", "범위주인");

        create(owner, "과거", LocalDateTime.now().minusMinutes(5), "").andExpect(status().isBadRequest());
        create(owner, "먼 미래", LocalDateTime.now().plusDays(120), "").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인하지 않으면 예약할 수 없고, 내 예약 목록도 볼 수 없다")
    void requiresLogin() throws Exception {

        mockMvc.perform(post("/api/lives/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"scheduledAt\":\"%s\"}".formatted(at(LocalDateTime.now().plusDays(1)))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/lives/schedules/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("한 채널이 걸어 둘 수 있는 예약은 20개까지다")
    void limitPerChannel() throws Exception {

        String owner = signupAndLogin("owner@limit.com", "한도주인");

        for (int i = 0; i < 20; i++) {
            createId(owner, "예약 " + i, LocalDateTime.now().plusDays(1).plusMinutes(i));
        }

        create(owner, "스물한 번째", LocalDateTime.now().plusDays(2), "").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("시각은 시간대를 따라 같은 순간으로 읽힌다 (다른 시간대로 적어도 어긋나지 않는다)")
    void timeIsReadAsAnInstant() throws Exception {

        String owner = signupAndLogin("owner@zone.com", "시간대주인");

        // 30분 뒤를 UTC-12 시계로 적으면 숫자상으로는 12시간 전이다. 시간대를 무시하고 읽으면 과거로 거절된다.
        java.time.OffsetDateTime thirtyMinutesLater = java.time.OffsetDateTime.now()
                .plusMinutes(30).withNano(0).withOffsetSameInstant(java.time.ZoneOffset.ofHours(-12));

        mockMvc.perform(post("/api/lives/schedules")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"먼 시간대\",\"scheduledAt\":\"%s\"}".formatted(
                                thirtyMinutesLater.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("시간대가 없는 시각은 받지 않는다")
    void timeWithoutZoneIsRejected() throws Exception {

        String owner = signupAndLogin("owner@nozone.com", "무시간대주인");

        mockMvc.perform(post("/api/lives/schedules")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"시간대 없음\",\"scheduledAt\":\"%s\"}"
                                .formatted(LocalDateTime.now().plusDays(1).withNano(0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("알림에 적히는 방송 시각은 한국 시간이다")
    void notificationShowsKoreanTime() throws Exception {

        String owner = signupAndLogin("owner@kst.com", "한국시간주인");
        String fan = signupAndLogin("fan@kst.com", "한국시간구독자");

        subscribe(fan, myUserId(owner));

        java.time.OffsetDateTime when = java.time.OffsetDateTime.now().plusDays(1).withNano(0);

        mockMvc.perform(post("/api/lives/schedules")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"한국 시각\",\"scheduledAt\":\"%s\"}"
                                .formatted(when.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME))))
                .andExpect(status().isCreated());

        String expected = when.atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                .format(java.time.format.DateTimeFormatter.ofPattern("M월 d일 HH:mm"));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.content[0].message").value(org.hamcrest.Matchers.containsString(expected)));
    }

    // ---- 알림 ----

    @Test
    @DisplayName("예약하면 구독자에게 알림이 가고, 구독하지 않은 사람에게는 가지 않는다")
    void subscribersAreNotified() throws Exception {

        String owner = signupAndLogin("owner@notify.com", "알림주인");
        String fan = signupAndLogin("fan@notify.com", "알림구독자");
        String stranger = signupAndLogin("stranger@notify.com", "알림외부인");

        subscribe(fan, myUserId(owner));

        long id = createId(owner, "알림 방송", LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.content[0].type").value("LIVE_SCHEDULED"))
                .andExpect(jsonPath("$.data.content[0].targetId").value(id))
                .andExpect(jsonPath("$.data.content[0].message").value(org.hamcrest.Matchers.containsString("예약")));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + stranger))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @DisplayName("시간을 바꾸면 다시 알리고, 제목만 고치면 알리지 않는다")
    void changeNotifiesOnlyWhenTimeChanges() throws Exception {

        String owner = signupAndLogin("owner@change.com", "변경주인");
        String fan = signupAndLogin("fan@change.com", "변경구독자");

        subscribe(fan, myUserId(owner));

        LocalDateTime when = LocalDateTime.now().plusDays(1);
        long id = createId(owner, "처음 제목", when);

        // 제목만 고침 — 알림은 처음 한 건뿐
        mockMvc.perform(put("/api/lives/schedules/" + id)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"새 제목\",\"scheduledAt\":\"%s\"}".formatted(at(when))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("새 제목"));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        // 시간을 바꿈 — 한 건 더
        mockMvc.perform(put("/api/lives/schedules/" + id)
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"새 제목\",\"scheduledAt\":\"%s\"}".formatted(at(when.plusHours(3)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].message").value(org.hamcrest.Matchers.containsString("바꿨")));
    }

    @Test
    @DisplayName("예약을 취소하면 목록에서 사라지고 구독자에게 알린다")
    void cancelRemovesAndNotifies() throws Exception {

        String owner = signupAndLogin("owner@cancel.com", "취소주인");
        String fan = signupAndLogin("fan@cancel.com", "취소구독자");

        subscribe(fan, myUserId(owner));

        long id = createId(owner, "취소될 방송", LocalDateTime.now().plusDays(1));

        mockMvc.perform(delete("/api/lives/schedules/" + id).header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/schedules"))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/lives/schedules/" + id))
                .andExpect(jsonPath("$.data.status").value("CANCELED"));

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + fan))
                .andExpect(jsonPath("$.data.content[0].message").value(org.hamcrest.Matchers.containsString("취소")));
    }

    @Test
    @DisplayName("남의 예약은 고치거나 취소할 수 없다")
    void cannotTouchOthersSchedule() throws Exception {

        String owner = signupAndLogin("owner@mine.com", "예약임자");
        String other = signupAndLogin("other@mine.com", "예약타인");

        long id = createId(owner, "내 예약", LocalDateTime.now().plusDays(1));

        mockMvc.perform(delete("/api/lives/schedules/" + id).header("Authorization", "Bearer " + other))
                .andExpect(status().isForbidden());
    }

    // ---- 목록 ----

    @Test
    @DisplayName("앞으로의 예약은 가까운 순으로 나오고, 내 예약 목록에는 내 것만 나온다")
    void listsAreOrdered() throws Exception {

        String a = signupAndLogin("a@list.com", "예약가");
        String b = signupAndLogin("b@list.com", "예약나");

        createId(a, "늦은 방송", LocalDateTime.now().plusDays(3));
        createId(b, "이른 방송", LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/lives/schedules"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].title").value("이른 방송"))
                .andExpect(jsonPath("$.data.content[1].title").value("늦은 방송"));

        mockMvc.perform(get("/api/lives/schedules/mine").header("Authorization", "Bearer " + a))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("늦은 방송"));

        mockMvc.perform(get("/api/channels/" + myUserId(b) + "/schedules"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("이른 방송"));
    }

    @Test
    @DisplayName("내가 차단한 채널의 예약은 목록에서 빠진다")
    void blockedChannelsAreHidden() throws Exception {

        String viewer = signupAndLogin("viewer@block.com", "차단시청자");
        String owner = signupAndLogin("owner@block.com", "차단주인");

        createId(owner, "숨겨질 방송", LocalDateTime.now().plusDays(1));

        mockMvc.perform(post("/api/users/" + myUserId(owner) + "/block").header("Authorization", "Bearer " + viewer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/schedules").header("Authorization", "Bearer " + viewer))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ---- 방송 시작 ----

    @Test
    @DisplayName("방송이 시작되면 가까운 예약이 그 방송으로 이어지고 예약의 제목·공개 대상이 쓰인다")
    void broadcastClaimsTheSchedule() throws Exception {

        String owner = signupAndLogin("owner@claim.com", "이어붙임주인");

        long id = json(create(owner, "예약한 제목", LocalDateTime.now().plusMinutes(20),
                ",\"audience\":\"PAID\",\"chatAudience\":\"SUBSCRIBERS\"")
                .andExpect(status().isCreated()).andReturn()).path("data").path("id").asLong();

        startBroadcast(owner);

        long liveId = currentLiveId(myUserId(owner));

        mockMvc.perform(get("/api/lives/" + liveId))
                .andExpect(jsonPath("$.data.title").value("예약한 제목"))
                .andExpect(jsonPath("$.data.audience").value("PAID"))
                .andExpect(jsonPath("$.data.chatAudience").value("SUBSCRIBERS"));

        mockMvc.perform(get("/api/lives/schedules/" + id))
                .andExpect(jsonPath("$.data.status").value("STARTED"))
                .andExpect(jsonPath("$.data.liveId").value(liveId));
    }

    @Test
    @DisplayName("한참 먼 예약은 지금 시작한 방송에 이어 붙지 않는다")
    void farScheduleIsNotClaimed() throws Exception {

        String owner = signupAndLogin("owner@far.com", "먼예약주인");

        long id = createId(owner, "내일 방송", LocalDateTime.now().plusDays(1));

        startBroadcast(owner);

        mockMvc.perform(get("/api/lives/schedules/" + id))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));

        mockMvc.perform(get("/api/channels/" + myUserId(owner) + "/live"))
                .andExpect(jsonPath("$.data.title").value("먼예약주인 님의 방송"));
    }

    // ---- 알림 작업 ----

    @Test
    @DisplayName("곧 시작하는 예약은 구독자에게 한 번만 알린다")
    void reminderIsSentOnce() throws Exception {

        String owner = signupAndLogin("owner@remind.com", "곧시작주인");
        String fan = signupAndLogin("fan@remind.com", "곧시작구독자");
        String far = signupAndLogin("far@remind.com", "먼구독자");

        subscribe(fan, myUserId(owner));
        subscribe(far, myUserId(owner));

        createId(owner, "곧 시작", LocalDateTime.now().plusMinutes(5));
        createId(owner, "한참 뒤", LocalDateTime.now().plusDays(2));

        assertThat(scheduleService.sendReminders()).isEqualTo(1);
        assertThat(scheduleService.sendReminders()).isZero();

        // 구독자에게 예약 알림 두 건 + 곧 시작 한 건
        JsonNode notifications = json(mockMvc.perform(
                        get("/api/notifications").header("Authorization", "Bearer " + fan))
                .andReturn()).path("data");

        assertThat(notifications.path("totalElements").asLong()).isEqualTo(3);

        long reminders = 0;

        for (JsonNode notification : notifications.path("content")) {
            if ("LIVE_REMINDER".equals(notification.path("type").asString())) {
                reminders++;
            }
        }

        assertThat(reminders).isEqualTo(1);
    }

    @Test
    @DisplayName("시각이 한참 지난 예약은 정리돼 목록에서 내려간다")
    void staleSchedulesExpire() throws Exception {

        String owner = signupAndLogin("owner@stale.com", "만료주인");

        User user = userRepository.findByEmail("owner@stale.com").orElseThrow();

        LiveSchedule stale = scheduleRepository.save(new LiveSchedule(
                user, "옛 예약", null, null, LocalDateTime.now().minusHours(10), Audience.ALL, Audience.ALL));

        assertThat(scheduleService.expireStale()).isEqualTo(1);

        mockMvc.perform(get("/api/lives/schedules/" + stale.getId()))
                .andExpect(jsonPath("$.data.status").value("EXPIRED"));

        mockMvc.perform(get("/api/lives/schedules/mine").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
