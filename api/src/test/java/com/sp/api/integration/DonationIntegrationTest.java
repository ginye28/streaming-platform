package com.sp.api.integration;

import com.sp.api.chat.event.ChatBroadcast;
import com.sp.api.chat.dto.ChatMessageResponse;
import com.sp.api.integration.MembershipPaymentIntegrationTest.FakeGateway;
import com.sp.api.integration.MembershipPaymentIntegrationTest.FakeGatewayConfig;
import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentKind;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 방송 후원(슈퍼챗). 결제사는 가짜(FakeGateway)다. 주문 → 승인 → 채팅에 후원 메시지가 올라가는지,
 * 환불하면 메시지가 지워지는지, 채팅이 막힌 사람은 돈을 내기 전에 걸러지는지를 확인한다.
 */
@RecordApplicationEvents
@Import(FakeGatewayConfig.class)
@TestPropertySource(properties = {
        "app.payments.toss.client-key=test_ck_dummy",
        "app.payments.toss.secret-key=test_sk_dummy",
        "app.donation.amounts=1000,5000,10000"
})
class DonationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FakeGateway gateway;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ApplicationEvents events;

    private String owner;
    private String donor;
    private long ownerId;
    private long liveId;

    @BeforeEach
    void setUp() throws Exception {

        gateway.reset();

        owner = signupAndLogin("owner@don.com", "후원받는이");
        donor = signupAndLogin("donor@don.com", "후원하는이");

        ownerId = myUserId(owner);

        startBroadcast(owner);
        liveId = currentLiveId(ownerId);
    }

    private ResultActions order(String token, long live, String body) throws Exception {
        return mockMvc.perform(post("/api/lives/" + live + "/donations/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String orderId(String token, int amount, String message) throws Exception {
        var result = order(token, liveId, "{\"amount\":%d,\"message\":\"%s\"}".formatted(amount, message))
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).path("data").path("orderId").asString();
    }

    private ResultActions confirm(String token, String paymentKey, String orderId, int amount) throws Exception {
        return mockMvc.perform(post("/api/payments/confirm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentKey\":\"%s\",\"orderId\":\"%s\",\"amount\":%d}"
                        .formatted(paymentKey, orderId, amount)));
    }

    // ---- 설정 · 주문 ----

    @Test
    @DisplayName("결제 설정에 후원으로 고를 수 있는 금액이 들어 있다")
    void configListsDonationAmounts() throws Exception {

        mockMvc.perform(get("/api/payments/config"))
                .andExpect(jsonPath("$.data.donationAmounts[0]").value(1000))
                .andExpect(jsonPath("$.data.donationAmounts[2]").value(10000))
                .andExpect(jsonPath("$.data.donationMessageMaxLength").value(100));
    }

    @Test
    @DisplayName("후원 주문은 서버가 정한 금액과 주문번호로 만들어진다")
    void orderIsCreated() throws Exception {

        order(donor, liveId, "{\"amount\":5000,\"message\":\"힘내세요\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(org.hamcrest.Matchers.startsWith("don-" + liveId + "-")))
                .andExpect(jsonPath("$.data.amount").value(5000))
                .andExpect(jsonPath("$.data.orderName").value("후원받는이 방송 후원"));

        Payment payment = paymentRepository.findAll().get(0);

        assertThat(payment.getKind()).isEqualTo(PaymentKind.DONATION);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getDonationMessage()).isEqualTo("힘내세요");
        assertThat(payment.getChannel().getId()).isEqualTo(ownerId);
    }

    @Test
    @DisplayName("허용되지 않은 금액으로는 주문을 만들 수 없다")
    void amountMustBeOneOfTheChoices() throws Exception {

        order(donor, liveId, "{\"amount\":1234}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자기 방송에는 후원할 수 없다")
    void cannotDonateToOwnLive() throws Exception {

        order(owner, liveId, "{\"amount\":1000}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인하지 않으면 후원할 수 없다")
    void donationRequiresLogin() throws Exception {

        mockMvc.perform(post("/api/lives/" + liveId + "/donations/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("끝난 방송에는 후원할 수 없다")
    void cannotDonateToEndedLive() throws Exception {

        endBroadcast(startedName());

        order(donor, liveId, "{\"amount\":1000}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("채팅이 막힌 사람은 돈을 내기 전에 주문 단계에서 거절된다")
    void restrictedUserIsRejectedBeforePaying() throws Exception {

        long donorId = myUserId(donor);

        mockMvc.perform(post("/api/lives/" + liveId + "/restrictions")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + donorId + "}"))
                .andExpect(status().isOk());

        order(donor, liveId, "{\"amount\":1000}").andExpect(status().isForbidden());

        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    @DisplayName("금칙어가 든 후원 메시지는 주문 단계에서 거절된다")
    void bannedWordIsRejectedBeforePaying() throws Exception {

        mockMvc.perform(post("/api/users/me/banned-words")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"word\":\"욕설\"}"))
                .andExpect(status().isCreated());

        order(donor, liveId, "{\"amount\":1000,\"message\":\"이건 욕 설 입니다\"}")
                .andExpect(status().isBadRequest());
    }

    // ---- 승인 ----

    @Test
    @DisplayName("승인되면 채팅에 후원 메시지가 올라가고 방송 주인에게 알림이 간다")
    void approvedDonationAppearsInChat() throws Exception {

        String orderId = orderId(donor, 10000, "응원합니다");

        confirm(donor, "pk-don-1", orderId, 10000)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("DONATION"))
                .andExpect(jsonPath("$.data.liveId").value(liveId))
                .andExpect(jsonPath("$.data.amount").value(10000))
                .andExpect(jsonPath("$.data.tier").doesNotExist());

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.content[0].content").value("응원합니다"))
                .andExpect(jsonPath("$.data.content[0].nickname").value("후원하는이"))
                .andExpect(jsonPath("$.data.content[0].donationAmount").value(10000))
                .andExpect(jsonPath("$.data.content[0].donationTier").value(3))
                .andExpect(jsonPath("$.data.content[0].donationPinRemainingSeconds")
                        .value(org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.greaterThan(170),
                                org.hamcrest.Matchers.lessThanOrEqualTo(180))));

        // 방송에도 후원 메시지가 실시간으로 나간다
        assertThat(events.stream(ChatBroadcast.class)
                .filter(broadcast -> broadcast.payload() instanceof ChatMessageResponse message
                        && message.donationAmount() != null && message.donationAmount() == 10000))
                .hasSize(1);

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.content[0].type").value("DONATION"))
                .andExpect(jsonPath("$.data.content[0].message")
                        .value(org.hamcrest.Matchers.containsString("10,000원")))
                .andExpect(jsonPath("$.data.content[0].targetId").value(liveId));
    }

    @Test
    @DisplayName("후원 승인은 구독 기간을 바꾸지 않는다")
    void donationDoesNotTouchSubscription() throws Exception {

        subscribe(donor, ownerId);

        confirm(donor, "pk-don-2", orderId(donor, 1000, "소액"), 1000).andExpect(status().isOk());

        mockMvc.perform(get("/api/channels/" + ownerId).header("Authorization", "Bearer " + donor))
                .andExpect(jsonPath("$.data.myTier").value("BASIC"))
                .andExpect(jsonPath("$.data.myPaidUntil").doesNotExist());
    }

    @Test
    @DisplayName("같은 주문을 다시 승인해도 후원 메시지가 두 번 올라가지 않는다")
    void confirmTwiceDoesNotDuplicate() throws Exception {

        String orderId = orderId(donor, 5000, "한 번만");

        confirm(donor, "pk-don-3", orderId, 5000).andExpect(status().isOk());
        confirm(donor, "pk-don-3", orderId, 5000).andExpect(status().isOk());

        assertThat(gateway.confirmCalls).isEqualTo(1);

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("메시지를 비워 둬도 후원은 올라간다")
    void donationWithoutMessage() throws Exception {

        String orderId = json(order(donor, liveId, "{\"amount\":1000}")
                .andExpect(status().isCreated()).andReturn()).path("data").path("orderId").asString();

        confirm(donor, "pk-don-4", orderId, 1000).andExpect(status().isOk());

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.content[0].donationAmount").value(1000))
                .andExpect(jsonPath("$.data.content[0].content").value(""));
    }

    // ---- 내역 · 환불 · 장부 ----

    @Test
    @DisplayName("결제 내역에는 후원이 후원으로 표시되고, 직접 환불은 할 수 없다")
    void historyMarksDonationAsNotRefundable() throws Exception {

        confirm(donor, "pk-don-5", orderId(donor, 5000, "내역"), 5000).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/payments").header("Authorization", "Bearer " + donor))
                .andExpect(jsonPath("$.data.content[0].kind").value("DONATION"))
                .andExpect(jsonPath("$.data.content[0].refundable").value(false));

        long paymentId = paymentRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/payments/" + paymentId + "/cancel")
                        .header("Authorization", "Bearer " + donor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(gateway.cancelCalls).isZero();
    }

    @Test
    @DisplayName("관리자가 환불하면 후원 메시지가 채팅에서 지워진다")
    void adminRefundRemovesChatMessage() throws Exception {

        String admin = adminToken("admin@don.com", "후원관리자");

        confirm(donor, "pk-don-6", orderId(donor, 5000, "환불될 후원"), 5000).andExpect(status().isOk());

        long paymentId = paymentRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/cancel")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"이중 결제\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.kind").value("DONATION"));

        mockMvc.perform(get("/api/lives/" + liveId + "/chats"))
                .andExpect(jsonPath("$.data.content[0].deleted").value(true))
                .andExpect(jsonPath("$.data.content[0].content").doesNotExist());
    }

    @Test
    @DisplayName("수익 장부는 후원과 구독을 나눠 보여 주고, 환불된 금액은 뺀다")
    void earningsSplitsDonationFromMembership() throws Exception {

        String admin = adminToken("admin2@don.com", "장부관리자");

        confirm(donor, "pk-don-7", orderId(donor, 10000, "일"), 10000).andExpect(status().isOk());
        confirm(donor, "pk-don-8", orderId(donor, 5000, "이"), 5000).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/earnings").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.total.gross").value(15000))
                .andExpect(jsonPath("$.data.total.donation").value(15000))
                .andExpect(jsonPath("$.data.total.membership").value(0));

        Payment first = paymentRepository.findAll().stream()
                .filter(payment -> payment.getAmount() == 10000).findFirst().orElseThrow();

        mockMvc.perform(post("/api/admin/payments/" + first.getId() + "/cancel")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"정정\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/earnings").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.total.refunded").value(10000))
                .andExpect(jsonPath("$.data.total.donation").value(5000))
                .andExpect(jsonPath("$.data.total.net").value(5000));
    }

    /** 지금 방송의 재생 이름. */
    private String startedName() throws Exception {
        String url = json(mockMvc.perform(get("/api/lives/" + liveId)).andReturn())
                .path("data").path("hlsUrl").asString();
        return url.substring(url.lastIndexOf('/') + 1).replace(".m3u8", "");
    }
}
