package com.sp.api.integration;

import com.sp.api.integration.MembershipPaymentIntegrationTest.FakeGateway;
import com.sp.api.integration.MembershipPaymentIntegrationTest.FakeGatewayConfig;
import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.gateway.GatewayPayment;
import com.sp.api.payment.gateway.PaymentGatewayException;
import com.sp.api.payment.repository.PaymentRepository;
import com.sp.api.payment.service.PaidExpiryService;
import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.repository.SubscribeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 결제 이후의 일들 — 환불, 토스 웹훅, 채널 주인 수익 장부, 유료 만료 임박 알림.
 * 가짜 결제사로 토스의 응답을 흉내 낸다. 한 트랜잭션에서 도는 테스트라, 예외를 일으키는 호출은
 * 테스트 하나에 한 번만 둔다.
 */
@Import(FakeGatewayConfig.class)
@TestPropertySource(properties = {
        "app.payments.toss.client-key=test_ck_dummy",
        "app.payments.toss.secret-key=test_sk_dummy",
        "app.membership.price-krw=4900",
        "app.membership.period-days=30",
        "app.membership.refund-window-days=7",
        "app.membership.platform-fee-percent=10",
        "app.membership.expiry-notice-days=3"
})
class PaymentExtrasIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FakeGateway gateway;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscribeRepository subscribeRepository;

    @Autowired
    private PaidExpiryService paidExpiryService;

    @BeforeEach
    void resetGateway() {
        gateway.reset();
    }

    // ---- 도우미 ----

    private String createOrder(String token, long channelId) throws Exception {
        return json(mockMvc.perform(post("/api/channels/" + channelId + "/membership/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn()).path("data").path("orderId").asString();
    }

    private ResultActions confirm(String token, String paymentKey, String orderId) throws Exception {
        return mockMvc.perform(post("/api/payments/confirm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentKey":"%s","orderId":"%s","amount":4900}""".formatted(paymentKey, orderId)));
    }

    /** 결제를 끝까지 해서 그 결제의 id 를 돌려준다. */
    private long pay(String token, long channelId, String paymentKey) throws Exception {
        String orderId = createOrder(token, channelId);
        confirm(token, paymentKey, orderId).andExpect(status().isOk());
        return paymentRepository.findByOrderId(orderId).orElseThrow().getId();
    }

    private ResultActions cancel(String token, long paymentId) throws Exception {
        return mockMvc.perform(post("/api/payments/" + paymentId + "/cancel")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"reason":"단순 변심"}"""));
    }

    private JsonNode channelView(String token, long channelId) throws Exception {
        return json(mockMvc.perform(get("/api/channels/" + channelId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).path("data");
    }

    private ResultActions webhook(String orderId, String paymentKey) throws Exception {
        return mockMvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"%s","paymentKey":"%s","status":"DONE"}}"""
                        .formatted(orderId, paymentKey)));
    }

    private void backdateApproval(long paymentId, int days) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        ReflectionTestUtils.setField(payment, "approvedAt", LocalDateTime.now().minusDays(days));
        paymentRepository.saveAndFlush(payment);
    }

    private String ownerToken;
    private long channelId;

    private void newChannel(String suffix) throws Exception {
        ownerToken = signupAndLogin("ex-owner" + suffix + "@test.com", "수익주인" + suffix);
        channelId = myUserId(ownerToken);
    }

    // ---- 환불 ----

    @Test
    @DisplayName("환불하면 토스 취소가 불리고, 결제는 CANCELED, 그 결제로 얻은 유료 기간은 사라진다")
    void userRefundRevertsPaidTime() throws Exception {

        newChannel("1");
        String fan = signupAndLogin("ex-fan1@test.com", "환불팬1");
        long paymentId = pay(fan, channelId, "pk_ex1");

        cancel(fan, paymentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.refundable").value(false));

        assertThat(gateway.cancelCalls).isEqualTo(1);

        JsonNode view = channelView(fan, channelId);
        // 구독은 남고, 유료만 사라진다
        assertThat(view.path("subscribedByMe").asBoolean()).isTrue();
        assertThat(view.path("myTier").asString()).isEqualTo("BASIC");

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELED);
    }

    @Test
    @DisplayName("결제를 두 번 했다가 한 번만 환불하면 그 한 번 몫(30일)만 줄어든다")
    void refundingOneOfTwoPaymentsKeepsTheRest() throws Exception {

        newChannel("2");
        String fan = signupAndLogin("ex-fan2@test.com", "환불팬2");

        long first = pay(fan, channelId, "pk_ex2a");
        pay(fan, channelId, "pk_ex2b");

        LocalDateTime before = LocalDateTime.parse(channelView(fan, channelId).path("myPaidUntil").asString());
        assertThat(before).isAfter(LocalDateTime.now().plusDays(59));

        cancel(fan, first).andExpect(status().isOk());

        JsonNode view = channelView(fan, channelId);
        assertThat(view.path("myTier").asString()).isEqualTo("PAID");
        assertThat(LocalDateTime.parse(view.path("myPaidUntil").asString())).isEqualTo(before.minusDays(30));
    }

    @Test
    @DisplayName("남의 결제는 환불할 수 없고 토스도 부르지 않는다")
    void cannotRefundSomeoneElsesPayment() throws Exception {

        newChannel("3");
        String fan = signupAndLogin("ex-fan3@test.com", "환불팬3");
        String other = signupAndLogin("ex-other3@test.com", "남3");
        long paymentId = pay(fan, channelId, "pk_ex3");

        cancel(other, paymentId).andExpect(status().isForbidden());

        assertThat(gateway.cancelCalls).isZero();
    }

    @Test
    @DisplayName("환불 가능 기간(7일)이 지나면 직접 환불할 수 없다")
    void refundWindowExpires() throws Exception {

        newChannel("4");
        String fan = signupAndLogin("ex-fan4@test.com", "환불팬4");
        long paymentId = pay(fan, channelId, "pk_ex4");
        backdateApproval(paymentId, 8);

        cancel(fan, paymentId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("7일")));

        assertThat(gateway.cancelCalls).isZero();
    }

    @Test
    @DisplayName("이미 환불된 결제는 다시 환불할 수 없다")
    void cannotRefundTwice() throws Exception {

        newChannel("5");
        String fan = signupAndLogin("ex-fan5@test.com", "환불팬5");
        long paymentId = pay(fan, channelId, "pk_ex5");

        cancel(fan, paymentId).andExpect(status().isOk());
        cancel(fan, paymentId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 환불된 결제입니다."));

        assertThat(gateway.cancelCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("승인되지 않은 주문은 환불할 수 없다")
    void cannotRefundUnpaidOrder() throws Exception {

        newChannel("6");
        String fan = signupAndLogin("ex-fan6@test.com", "환불팬6");
        String orderId = createOrder(fan, channelId);
        long paymentId = paymentRepository.findByOrderId(orderId).orElseThrow().getId();

        cancel(fan, paymentId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("승인된 결제만 환불할 수 있습니다."));
    }

    @Test
    @DisplayName("토스가 취소를 거절하면 결제와 유료 구독은 그대로다")
    void gatewayRefusalKeepsEverything() throws Exception {

        newChannel("7");
        String fan = signupAndLogin("ex-fan7@test.com", "환불팬7");
        long paymentId = pay(fan, channelId, "pk_ex7");

        gateway.onCancel = key -> {
            throw new PaymentGatewayException("NOT_CANCELABLE_AMOUNT", "취소할 수 없는 결제입니다.", false);
        };

        cancel(fan, paymentId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("취소할 수 없는 결제입니다."));

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(channelView(fan, channelId).path("myTier").asString()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("토스 장애면 503 이고 결제는 그대로라 다시 시도할 수 있다")
    void gatewayOutageKeepsPaymentRefundable() throws Exception {

        newChannel("8");
        String fan = signupAndLogin("ex-fan8@test.com", "환불팬8");
        long paymentId = pay(fan, channelId, "pk_ex8");

        gateway.onCancel = key -> {
            throw new PaymentGatewayException("TOSS_UNAVAILABLE", "결제사에 연결하지 못했습니다.", true);
        };

        cancel(fan, paymentId).andExpect(status().isServiceUnavailable());

        assertThat(paymentRepository.findById(paymentId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.DONE);
    }

    @Test
    @DisplayName("토스가 이미 취소됐다고 하면 우리 쪽만 맞춘다")
    void alreadyCanceledAtTossIsSyncedLocally() throws Exception {

        newChannel("9");
        String fan = signupAndLogin("ex-fan9@test.com", "환불팬9");
        long paymentId = pay(fan, channelId, "pk_ex9");

        gateway.onCancel = key -> {
            throw new PaymentGatewayException("ALREADY_CANCELED_PAYMENT", "이미 취소된 결제입니다.", false);
        };

        cancel(fan, paymentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    @Test
    @DisplayName("내 결제 내역에 환불 가능 여부와 마감이 나오고, 환불된 결제도 남는다")
    void historyShowsRefundability() throws Exception {

        newChannel("10");
        String fan = signupAndLogin("ex-fan10@test.com", "환불팬10");
        long paid = pay(fan, channelId, "pk_ex10a");
        long other = pay(fan, channelId, "pk_ex10b");
        cancel(fan, other).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/payments").header("Authorization", "Bearer " + fan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                // 최신순: 방금 환불한 것이 먼저
                .andExpect(jsonPath("$.data.content[0].id").value(other))
                .andExpect(jsonPath("$.data.content[0].status").value("CANCELED"))
                .andExpect(jsonPath("$.data.content[0].refundable").value(false))
                .andExpect(jsonPath("$.data.content[1].id").value(paid))
                .andExpect(jsonPath("$.data.content[1].status").value("DONE"))
                .andExpect(jsonPath("$.data.content[1].refundable").value(true))
                .andExpect(jsonPath("$.data.content[1].refundDeadline").isString());
    }

    // ---- 관리자 환불 ----

    @Test
    @DisplayName("관리자는 환불 가능 기간이 지나도 사유와 함께 환불할 수 있다")
    void adminRefundIgnoresWindow() throws Exception {

        newChannel("11");
        String fan = signupAndLogin("ex-fan11@test.com", "환불팬11");
        long paymentId = pay(fan, channelId, "pk_ex11");
        backdateApproval(paymentId, 30);
        String admin = adminToken("ex-admin11@test.com", "관리자11");

        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/cancel")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"이중 결제 확인"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.cancelReason").value("이중 결제 확인"))
                .andExpect(jsonPath("$.data.userEmail").value("ex-fan11@test.com"));

        assertThat(channelView(fan, channelId).path("myTier").asString()).isEqualTo("BASIC");
    }

    @Test
    @DisplayName("관리자 환불에는 사유가 필요하다")
    void adminRefundNeedsReason() throws Exception {

        newChannel("12");
        String fan = signupAndLogin("ex-fan12@test.com", "환불팬12");
        long paymentId = pay(fan, channelId, "pk_ex12");
        String admin = adminToken("ex-admin12@test.com", "관리자12");

        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/cancel")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(gateway.cancelCalls).isZero();
    }

    @Test
    @DisplayName("관리자 결제 API 는 관리자만 쓸 수 있다")
    void adminPaymentApiIsAdminOnly() throws Exception {

        newChannel("13");
        String fan = signupAndLogin("ex-fan13@test.com", "환불팬13");
        long paymentId = pay(fan, channelId, "pk_ex13");

        mockMvc.perform(get("/api/admin/payments").header("Authorization", "Bearer " + fan))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/payments/" + paymentId + "/cancel")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"x"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payments")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("관리자 목록은 상태로 거를 수 있고 결제한 사람과 채널이 보인다")
    void adminListFiltersByStatus() throws Exception {

        newChannel("14");
        String fan = signupAndLogin("ex-fan14@test.com", "환불팬14");
        pay(fan, channelId, "pk_ex14a");
        long second = pay(fan, channelId, "pk_ex14b");
        cancel(fan, second).andExpect(status().isOk());
        String admin = adminToken("ex-admin14@test.com", "관리자14");

        mockMvc.perform(get("/api/admin/payments?status=CANCELED").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("CANCELED"))
                .andExpect(jsonPath("$.data.content[0].channelNickname").value("수익주인14"));

        mockMvc.perform(get("/api/admin/payments?status=DONE").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    // ---- 웹훅 ----

    @Test
    @DisplayName("결제창에서 돌아오지 못했어도 웹훅이 오면 토스에 확인한 뒤 유료 구독을 시작한다")
    void webhookRescuesApprovalThatNeverReturned() throws Exception {

        newChannel("15");
        String fan = signupAndLogin("ex-fan15@test.com", "웹훅팬15");
        String orderId = createOrder(fan, channelId);

        gateway.onFind = key -> FakeGateway.done(key, orderId, 4900);

        // 로그인 없이 토스가 부른다
        webhook(orderId, "pk_wh15").andExpect(status().isOk());

        assertThat(gateway.findCalls).isEqualTo(1);
        assertThat(channelView(fan, channelId).path("myTier").asString()).isEqualTo("PAID");
        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.DONE);
    }

    @Test
    @DisplayName("웹훅이 DONE 이라고 해도 토스 조회가 아니면 믿지 않는다")
    void webhookBodyIsNotTrusted() throws Exception {

        newChannel("16");
        String fan = signupAndLogin("ex-fan16@test.com", "웹훅팬16");
        String orderId = createOrder(fan, channelId);

        // 본문은 DONE 이라지만 토스에서는 승인되지 않은 결제다
        gateway.onFind = key -> new GatewayPayment(key, orderId, "READY", 4900, null, null, null);

        webhook(orderId, "pk_wh16").andExpect(status().isOk());

        assertThat(channelView(fan, channelId).path("subscribedByMe").asBoolean()).isFalse();
        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.READY);
    }

    @Test
    @DisplayName("웹훅이 알려 준 결제 금액이 주문과 다르면 구독을 주지 않는다")
    void webhookWithMismatchedAmountGrantsNothing() throws Exception {

        newChannel("17");
        String fan = signupAndLogin("ex-fan17@test.com", "웹훅팬17");
        String orderId = createOrder(fan, channelId);

        gateway.onFind = key -> FakeGateway.done(key, orderId, 100);

        webhook(orderId, "pk_wh17").andExpect(status().isOk());

        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.READY);
    }

    @Test
    @DisplayName("상점관리자에서 직접 취소했다는 웹훅이 오면 우리 쪽도 취소하고 유료 기간을 되돌린다")
    void webhookSyncsCancellationMadeAtToss() throws Exception {

        newChannel("18");
        String fan = signupAndLogin("ex-fan18@test.com", "웹훅팬18");
        String orderId = createOrder(fan, channelId);
        confirm(fan, "pk_wh18", orderId).andExpect(status().isOk());

        gateway.onFind = key -> new GatewayPayment(key, orderId, "CANCELED", 4900, "카드", null, null);

        webhook(orderId, "pk_wh18").andExpect(status().isOk());

        // 우리 쪽에서 토스를 다시 취소하지는 않는다
        assertThat(gateway.cancelCalls).isZero();
        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(channelView(fan, channelId).path("myTier").asString()).isEqualTo("BASIC");
    }

    @Test
    @DisplayName("모르는 주문이나 관심 없는 이벤트는 200 으로 받고 토스를 부르지 않는다")
    void webhookIgnoresUnknownThings() throws Exception {

        webhook("sub-0-unknown", "pk_none").andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventType":"DEPOSIT_CALLBACK","data":{"orderId":"x"}}"""))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        assertThat(gateway.findCalls).isZero();
    }

    @Test
    @DisplayName("웹훅을 처리하다 토스에 닿지 못하면 503 — 토스가 나중에 다시 보낸다")
    void webhookOutageAsksTossToRetry() throws Exception {

        newChannel("19");
        String fan = signupAndLogin("ex-fan19@test.com", "웹훅팬19");
        String orderId = createOrder(fan, channelId);

        gateway.onFind = key -> {
            throw new PaymentGatewayException("TOSS_UNAVAILABLE", "연결 실패", true);
        };

        webhook(orderId, "pk_wh19").andExpect(status().isServiceUnavailable());
    }

    // ---- 수익 장부 ----

    @Test
    @DisplayName("수익 장부는 달별 결제·환불·수수료·정산 예정액을 보여 주고 결제한 사람은 드러내지 않는다")
    void earningsLedger() throws Exception {

        newChannel("20");
        String fanA = signupAndLogin("ex-fan20a@test.com", "장부팬A");
        String fanB = signupAndLogin("ex-fan20b@test.com", "장부팬B");
        pay(fanA, channelId, "pk_ex20a");
        long refunded = pay(fanB, channelId, "pk_ex20b");
        cancel(fanB, refunded).andExpect(status().isOk());

        // 결제 둘, 환불 하나, 수수료 10% — (9800 - 4900) * 10% = 490, 정산 예정 4900 - 490 = 4410
        var result = mockMvc.perform(get("/api/users/me/earnings").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feePercent").value(10))
                .andExpect(jsonPath("$.data.total.gross").value(9800))
                .andExpect(jsonPath("$.data.total.refunded").value(4900))
                .andExpect(jsonPath("$.data.total.fee").value(490))
                .andExpect(jsonPath("$.data.total.net").value(4410))
                .andExpect(jsonPath("$.data.total.count").value(2))
                .andExpect(jsonPath("$.data.months[0].month").value(
                        LocalDateTime.now().toLocalDate().toString().substring(0, 7)))
                .andExpect(jsonPath("$.data.months[0].net").value(4410))
                .andReturn();

        // 누가 결제했는지는 어디에도 없다
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("ex-fan20").doesNotContain("장부팬");
    }

    @Test
    @DisplayName("결제를 받은 적 없는 채널의 수익 장부는 비어 있다")
    void emptyEarnings() throws Exception {

        String nobody = signupAndLogin("ex-nobody@test.com", "무수익");

        mockMvc.perform(get("/api/users/me/earnings").header("Authorization", "Bearer " + nobody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total.gross").value(0))
                .andExpect(jsonPath("$.data.total.net").value(0))
                .andExpect(jsonPath("$.data.months").isEmpty());

        mockMvc.perform(get("/api/users/me/earnings")).andExpect(status().isUnauthorized());
    }

    // ---- 만료 임박 알림 ----

    private void setPaidUntil(String token, long channel, LocalDateTime until) throws Exception {
        Subscribe subscribe = subscribeRepository
                .findBySubscriberIdAndChannelId(myUserId(token), channel).orElseThrow();
        ReflectionTestUtils.setField(subscribe, "paidUntil", until);
        subscribeRepository.saveAndFlush(subscribe);
    }

    private JsonNode notificationsOf(String token) throws Exception {
        return json(mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("content");
    }

    @Test
    @DisplayName("유료가 3일 안에 끝나는 사람에게만 알리고, 같은 만료로 두 번 알리지 않는다")
    void expiryNoticeIsSentOncePerExpiry() throws Exception {

        newChannel("21");
        String soon = signupAndLogin("ex-soon@test.com", "곧끝남");
        String later = signupAndLogin("ex-later@test.com", "한참남음");
        String over = signupAndLogin("ex-over@test.com", "이미끝남");
        pay(soon, channelId, "pk_soon");
        pay(later, channelId, "pk_later");
        pay(over, channelId, "pk_over");

        setPaidUntil(soon, channelId, LocalDateTime.now().plusDays(2));
        setPaidUntil(later, channelId, LocalDateTime.now().plusDays(20));
        setPaidUntil(over, channelId, LocalDateTime.now().minusDays(1));

        assertThat(paidExpiryService.notifyExpiring()).isEqualTo(1);
        // 다시 돌려도 같은 만료에 대해서는 알리지 않는다
        assertThat(paidExpiryService.notifyExpiring()).isZero();

        JsonNode notices = notificationsOf(soon);
        assertThat(notices).hasSize(1);
        assertThat(notices.get(0).path("type").asString()).isEqualTo("PAID_EXPIRING");
        assertThat(notices.get(0).path("channelId").asLong()).isEqualTo(channelId);
        assertThat(notices.get(0).path("message").asString()).contains("수익주인21").contains("끝납니다");

        assertThat(notificationsOf(later)).isEmpty();
        assertThat(notificationsOf(over)).isEmpty();
    }

    @Test
    @DisplayName("연장해서 만료가 바뀌면 다음 만료 때 다시 알린다")
    void expiryNoticeIsSentAgainAfterRenewal() throws Exception {

        newChannel("22");
        String fan = signupAndLogin("ex-fan22@test.com", "연장팬");
        pay(fan, channelId, "pk_renew1");
        setPaidUntil(fan, channelId, LocalDateTime.now().plusDays(1));

        assertThat(paidExpiryService.notifyExpiring()).isEqualTo(1);

        // 연장한 뒤 그 만료가 다시 임박한 상황
        setPaidUntil(fan, channelId, LocalDateTime.now().plusDays(2));

        assertThat(paidExpiryService.notifyExpiring()).isEqualTo(1);
        assertThat(notificationsOf(fan)).hasSize(2);
    }
}
