package com.sp.api.integration;

import com.sp.api.payment.entity.Payment;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.gateway.GatewayPayment;
import com.sp.api.payment.gateway.PaymentGateway;
import com.sp.api.payment.gateway.PaymentGatewayException;
import com.sp.api.payment.repository.PaymentRepository;
import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.subscribe.repository.SubscribeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 토스페이먼츠 유료 구독 결제. 진짜 결제사를 부르지 않고 가짜 결제사(FakeGateway)로
 * 승인 성공·거절·장애를 흉내 내어 서버의 판단만 확인한다.
 *
 * 테스트는 한 트랜잭션 안에서 도므로, 실패(예외)를 일으키는 호출은 테스트 하나에 한 번만 둔다.
 * 실제 서비스에서는 요청마다 트랜잭션이 따로라 이런 제약이 없다.
 */
@Import(MembershipPaymentIntegrationTest.FakeGatewayConfig.class)
@TestPropertySource(properties = {
        "app.payments.toss.client-key=test_ck_dummy",
        "app.payments.toss.secret-key=test_sk_dummy",
        "app.membership.price-krw=4900",
        "app.membership.period-days=30"
})
class MembershipPaymentIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FakeGateway gateway;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscribeRepository subscribeRepository;

    @BeforeEach
    void resetGateway() {
        gateway.reset();
    }

    // ---- 가짜 결제사 ----

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeGatewayConfig {

        @Bean
        @Primary
        FakeGateway fakeGateway() {
            return new FakeGateway();
        }
    }

    static class FakeGateway implements PaymentGateway {

        int confirmCalls;
        int findCalls;
        int cancelCalls;

        /** 승인 요청이 들어왔을 때 돌려줄 결과를 정한다. 예외를 던져 거절·장애를 흉내 낸다. */
        Function<String[], GatewayPayment> onConfirm;
        Function<String, GatewayPayment> onFind;
        /** 취소 요청이 들어왔을 때 돌려줄 결과. 기본은 취소 성공. */
        Function<String, GatewayPayment> onCancel;

        void reset() {
            confirmCalls = 0;
            findCalls = 0;
            cancelCalls = 0;
            onCancel = paymentKey -> new GatewayPayment(paymentKey, "ignored", "CANCELED", 0, "카드", null,
                    LocalDateTime.now());
            onConfirm = args -> done(args[0], args[1], Long.parseLong(args[2]));
            onFind = paymentKey -> {
                throw new IllegalStateException("조회하면 안 되는 테스트");
            };
        }

        static GatewayPayment done(String paymentKey, String orderId, long amount) {
            return new GatewayPayment(paymentKey, orderId, "DONE", amount, "카드",
                    "https://receipt.test/" + orderId, LocalDateTime.now());
        }

        @Override
        public GatewayPayment confirm(String paymentKey, String orderId, long amount) {
            confirmCalls++;
            return onConfirm.apply(new String[]{paymentKey, orderId, String.valueOf(amount)});
        }

        @Override
        public GatewayPayment find(String paymentKey) {
            findCalls++;
            return onFind.apply(paymentKey);
        }

        @Override
        public GatewayPayment cancel(String paymentKey, String reason) {
            cancelCalls++;
            return onCancel.apply(paymentKey);
        }
    }

    // ---- 도우미 ----

    private String createOrder(String token, long channelId) throws Exception {
        var result = mockMvc.perform(post("/api/channels/" + channelId + "/membership/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).path("data").path("orderId").asString();
    }

    private ResultActions confirm(String token, String paymentKey, String orderId, long amount) throws Exception {
        return mockMvc.perform(post("/api/payments/confirm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentKey":"%s","orderId":"%s","amount":%d}""".formatted(paymentKey, orderId, amount)));
    }

    private JsonNode myChannelView(String token, long channelId) throws Exception {
        return json(mockMvc.perform(get("/api/channels/" + channelId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).path("data");
    }

    // ---- 설정 · 주문 ----

    @Test
    @DisplayName("결제 설정은 로그인 없이 읽을 수 있고, 켜져 있으면 클라이언트 키와 금액을 준다")
    void configIsPublic() throws Exception {

        mockMvc.perform(get("/api/payments/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.clientKey").value("test_ck_dummy"))
                .andExpect(jsonPath("$.data.priceKrw").value(4900))
                .andExpect(jsonPath("$.data.periodDays").value(30))
                // 시크릿 키는 어떤 응답에도 없어야 한다
                .andExpect(jsonPath("$.data.secretKey").doesNotExist());
    }

    @Test
    @DisplayName("주문의 금액은 서버가 정한다 — 요청에는 금액을 보낼 곳이 없다")
    void orderAmountComesFromServer() throws Exception {

        String owner = signupAndLogin("pay-owner1@test.com", "결제주인1");
        String fan = signupAndLogin("pay-fan1@test.com", "결제팬1");

        mockMvc.perform(post("/api/channels/" + myUserId(owner) + "/membership/orders")
                        .header("Authorization", "Bearer " + fan))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(4900))
                .andExpect(jsonPath("$.data.orderName").value("결제주인1 유료 구독 30일"))
                .andExpect(jsonPath("$.data.customerEmail").value("pay-fan1@test.com"))
                .andExpect(jsonPath("$.data.orderId").isString());
    }

    @Test
    @DisplayName("주문번호는 토스 규칙(6~64자, 영문·숫자·-_=)에 맞는다")
    void orderIdFollowsTossRules() throws Exception {

        String owner = signupAndLogin("pay-owner2@test.com", "결제주인2");
        String fan = signupAndLogin("pay-fan2@test.com", "결제팬2");

        String orderId = createOrder(fan, myUserId(owner));

        assertThat(orderId).matches("[A-Za-z0-9_=-]{6,64}");
    }

    @Test
    @DisplayName("자기 채널은 결제할 수 없다")
    void cannotBuyOwnChannel() throws Exception {

        String owner = signupAndLogin("pay-owner3@test.com", "결제주인3");

        mockMvc.perform(post("/api/channels/" + myUserId(owner) + "/membership/orders")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 없이는 주문도 승인도 내역도 안 된다")
    void paymentApisNeedLogin() throws Exception {

        mockMvc.perform(post("/api/channels/1/membership/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentKey":"k","orderId":"o","amount":4900}"""))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/payments")).andExpect(status().isUnauthorized());
    }

    // ---- 승인 ----

    @Test
    @DisplayName("승인되면 유료 구독이 30일 시작되고, 구독하지 않았어도 구독이 함께 만들어진다")
    void confirmStartsPaidSubscription() throws Exception {

        String owner = signupAndLogin("pay-owner4@test.com", "결제주인4");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan4@test.com", "결제팬4");

        String orderId = createOrder(fan, channelId);

        confirm(fan, "pk_test_4", orderId, 4900)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("PAID"))
                .andExpect(jsonPath("$.data.amount").value(4900))
                .andExpect(jsonPath("$.data.method").value("카드"))
                .andExpect(jsonPath("$.data.receiptUrl").value("https://receipt.test/" + orderId));

        assertThat(gateway.confirmCalls).isEqualTo(1);

        JsonNode view = myChannelView(fan, channelId);
        assertThat(view.path("subscribedByMe").asBoolean()).isTrue();
        assertThat(view.path("myTier").asString()).isEqualTo("PAID");

        LocalDateTime paidUntil = LocalDateTime.parse(view.path("myPaidUntil").asString());
        assertThat(paidUntil).isAfter(LocalDateTime.now().plusDays(29));
        assertThat(paidUntil).isBefore(LocalDateTime.now().plusDays(31));

        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.DONE);
    }

    @Test
    @DisplayName("유료 기간이 남아 있을 때 또 결제하면 남은 기간 뒤에 30일이 이어진다")
    void secondPaymentExtendsFromRemainingTime() throws Exception {

        String owner = signupAndLogin("pay-owner5@test.com", "결제주인5");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan5@test.com", "결제팬5");

        confirm(fan, "pk_5a", createOrder(fan, channelId), 4900).andExpect(status().isOk());
        LocalDateTime first = LocalDateTime.parse(myChannelView(fan, channelId).path("myPaidUntil").asString());

        confirm(fan, "pk_5b", createOrder(fan, channelId), 4900).andExpect(status().isOk());
        LocalDateTime second = LocalDateTime.parse(myChannelView(fan, channelId).path("myPaidUntil").asString());

        assertThat(second).isEqualTo(first.plusDays(30));
    }

    @Test
    @DisplayName("같은 결제를 다시 승인해도 한 번만 청구되고 기간도 한 번만 늘어난다")
    void confirmIsIdempotent() throws Exception {

        String owner = signupAndLogin("pay-owner6@test.com", "결제주인6");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan6@test.com", "결제팬6");
        String orderId = createOrder(fan, channelId);

        confirm(fan, "pk_6", orderId, 4900).andExpect(status().isOk());
        String once = myChannelView(fan, channelId).path("myPaidUntil").asString();

        // 결제 성공 페이지를 새로고침한 상황
        confirm(fan, "pk_6", orderId, 4900)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("PAID"));

        assertThat(gateway.confirmCalls).isEqualTo(1);
        assertThat(myChannelView(fan, channelId).path("myPaidUntil").asString()).isEqualTo(once);
    }

    @Test
    @DisplayName("결제창에서 돌아온 금액이 주문과 다르면 결제사를 부르지 않고 막는다")
    void rejectsTamperedAmountBeforeCallingGateway() throws Exception {

        String owner = signupAndLogin("pay-owner7@test.com", "결제주인7");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan7@test.com", "결제팬7");
        String orderId = createOrder(fan, channelId);

        confirm(fan, "pk_7", orderId, 100)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("결제 금액이 주문과 다릅니다."));

        assertThat(gateway.confirmCalls).isZero();
        assertThat(myChannelView(fan, channelId).path("subscribedByMe").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("남의 주문은 승인할 수 없다")
    void cannotConfirmSomeoneElsesOrder() throws Exception {

        String owner = signupAndLogin("pay-owner8@test.com", "결제주인8");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan8@test.com", "결제팬8");
        String thief = signupAndLogin("pay-thief8@test.com", "남의결제");
        String orderId = createOrder(fan, channelId);

        confirm(thief, "pk_8", orderId, 4900).andExpect(status().isForbidden());

        assertThat(gateway.confirmCalls).isZero();
    }

    @Test
    @DisplayName("없는 주문은 404")
    void unknownOrderIsNotFound() throws Exception {

        String fan = signupAndLogin("pay-fan9@test.com", "결제팬9");

        confirm(fan, "pk_9", "sub-0-doesnotexist", 4900).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("결제사가 거절하면 그 사유를 알려 주고 주문은 실패로 남으며 구독은 생기지 않는다")
    void gatewayRejectionFailsTheOrder() throws Exception {

        String owner = signupAndLogin("pay-owner10@test.com", "결제주인10");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan10@test.com", "결제팬10");
        String orderId = createOrder(fan, channelId);

        gateway.onConfirm = args -> {
            throw new PaymentGatewayException("REJECT_CARD_PAYMENT", "카드 한도를 초과했습니다.", false);
        };

        confirm(fan, "pk_10", orderId, 4900)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("카드 한도를 초과했습니다."));

        Payment payment = paymentRepository.findByOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureCode()).isEqualTo("REJECT_CARD_PAYMENT");
        assertThat(subscribeRepository.findBySubscriberIdAndChannelId(myUserId(fan), channelId)).isEmpty();
    }

    @Test
    @DisplayName("결제사 장애면 실패로 확정하지 않고 503 — 같은 주문으로 다시 시도할 수 있다")
    void gatewayOutageKeepsOrderRetryable() throws Exception {

        String owner = signupAndLogin("pay-owner11@test.com", "결제주인11");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan11@test.com", "결제팬11");
        String orderId = createOrder(fan, channelId);

        gateway.onConfirm = args -> {
            throw new PaymentGatewayException("TOSS_UNAVAILABLE", "결제사에 연결하지 못했습니다.", true);
        };

        confirm(fan, "pk_11", orderId, 4900).andExpect(status().isServiceUnavailable());

        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.READY);
    }

    @Test
    @DisplayName("결제사가 이미 승인했다고 하면 결제를 조회해 확인한 뒤 구독을 시작한다")
    void alreadyProcessedIsRecheckedThenGranted() throws Exception {

        String owner = signupAndLogin("pay-owner12@test.com", "결제주인12");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan12@test.com", "결제팬12");
        String orderId = createOrder(fan, channelId);

        // 승인 응답만 놓친 상황: 승인 요청은 "이미 처리됨" 이고, 조회하면 DONE 이다
        gateway.onConfirm = args -> {
            throw new PaymentGatewayException("ALREADY_PROCESSED_PAYMENT", "이미 처리된 결제 입니다.", false);
        };
        gateway.onFind = paymentKey -> FakeGateway.done(paymentKey, orderId, 4900);

        confirm(fan, "pk_12", orderId, 4900)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("PAID"));

        assertThat(gateway.findCalls).isEqualTo(1);
    }

    @Test
    @DisplayName("결제사 응답의 금액이 주문과 다르면 구독을 주지 않는다")
    void mismatchedGatewayAmountGrantsNothing() throws Exception {

        String owner = signupAndLogin("pay-owner13@test.com", "결제주인13");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan13@test.com", "결제팬13");
        String orderId = createOrder(fan, channelId);

        gateway.onConfirm = args -> FakeGateway.done(args[0], args[1], 1L);

        confirm(fan, "pk_13", orderId, 4900).andExpect(status().isBadRequest());

        assertThat(paymentRepository.findByOrderId(orderId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);
        assertThat(subscribeRepository.findBySubscriberIdAndChannelId(myUserId(fan), channelId)).isEmpty();
    }

    // ---- 유료 기간 ----

    @Test
    @DisplayName("유료 기간이 지나면 일반 구독으로 보이고 마크도 일반 마크가 된다")
    void expiredPaidFallsBackToBasic() throws Exception {

        String owner = signupAndLogin("pay-owner14@test.com", "결제주인14");
        long channelId = myUserId(owner);
        mockMvc.perform(put("/api/users/me/channel-profile")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"oshiMarkUrl":"/uploads/basic.png","paidOshiMarkUrl":"/uploads/paid.png"}"""))
                .andExpect(status().isOk());
        long streamId = createStream(owner, "영상14");

        String fan = signupAndLogin("pay-fan14@test.com", "결제팬14");
        confirm(fan, "pk_14", createOrder(fan, channelId), 4900).andExpect(status().isOk());

        mockMvc.perform(post("/api/streams/" + streamId + "/comments")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"유료일 때"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.oshiMarkUrl").value("/uploads/paid.png"));

        // 유료 기간이 어제 끝난 것으로 만든다
        Subscribe subscribe = subscribeRepository
                .findBySubscriberIdAndChannelId(myUserId(fan), channelId).orElseThrow();
        ReflectionTestUtils.setField(subscribe, "paidUntil", LocalDateTime.now().minusDays(1));
        subscribeRepository.saveAndFlush(subscribe);

        JsonNode view = myChannelView(fan, channelId);
        assertThat(view.path("myTier").asString()).isEqualTo("BASIC");
        assertThat(view.path("myPaidUntil").isNull() || view.path("myPaidUntil").isMissingNode()).isTrue();

        mockMvc.perform(get("/api/streams/" + streamId + "/comments"))
                .andExpect(jsonPath("$.data.content[0].oshiMarkUrl").value("/uploads/basic.png"))
                .andExpect(jsonPath("$.data.content[0].oshiTier").value("BASIC"));
    }

    // ---- 설정 API 와의 관계 ----

    @Test
    @DisplayName("결제가 켜져 있으면 PUT 으로 직접 유료가 될 수 없다")
    void cannotSelfUpgradeWhenPaymentsAreOn() throws Exception {

        String owner = signupAndLogin("pay-owner15@test.com", "결제주인15");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan15@test.com", "결제팬15");

        mockMvc.perform(post("/api/channels/" + channelId + "/subscribe")
                        .header("Authorization", "Bearer " + fan))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/channels/" + channelId + "/subscription")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tier":"PAID"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("유료 구독은 결제로만 시작할 수 있습니다."));
    }

    @Test
    @DisplayName("일반으로 내리면 남은 유료 기간이 없어지고, 일반으로 내리는 것은 결제가 켜져 있어도 된다")
    void downgradeClearsPaidTime() throws Exception {

        String owner = signupAndLogin("pay-owner16@test.com", "결제주인16");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan16@test.com", "결제팬16");

        confirm(fan, "pk_16", createOrder(fan, channelId), 4900).andExpect(status().isOk());

        mockMvc.perform(put("/api/channels/" + channelId + "/subscription")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tier":"BASIC"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("BASIC"))
                .andExpect(jsonPath("$.data.paidUntil").doesNotExist());
    }

    // ---- 내역 ----

    @Test
    @DisplayName("내 결제 내역에는 승인된 결제만 최신순으로 나온다")
    void historyListsDonePaymentsOnly() throws Exception {

        String owner = signupAndLogin("pay-owner17@test.com", "결제주인17");
        long channelId = myUserId(owner);
        String fan = signupAndLogin("pay-fan17@test.com", "결제팬17");

        createOrder(fan, channelId); // 결제하지 않은 주문은 내역에 없다
        confirm(fan, "pk_17", createOrder(fan, channelId), 4900).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/payments").header("Authorization", "Bearer " + fan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].channelNickname").value("결제주인17"))
                .andExpect(jsonPath("$.data.content[0].amount").value(4900))
                .andExpect(jsonPath("$.data.content[0].receiptUrl").isString());

        // 다른 사람의 내역에는 보이지 않는다
        mockMvc.perform(get("/api/users/me/payments").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
