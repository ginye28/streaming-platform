package com.sp.api.integration;

import com.sp.api.integration.MembershipPaymentIntegrationTest.FakeGateway;
import com.sp.api.integration.MembershipPaymentIntegrationTest.FakeGatewayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 웹훅 토큰을 설정했을 때: 토큰이 없거나 틀리면 막고, 맞으면 받는다. */
@Import(FakeGatewayConfig.class)
@TestPropertySource(properties = {
        "app.payments.toss.client-key=test_ck_dummy",
        "app.payments.toss.secret-key=test_sk_dummy",
        "app.payments.toss.webhook-token=s3cret-token"
})
class PaymentWebhookTokenIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private FakeGateway gateway;

    @BeforeEach
    void reset() {
        gateway.reset();
    }

    private static final String BODY = """
            {"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"sub-0-x","paymentKey":"pk"}}""";

    @Test
    @DisplayName("토큰이 없거나 틀리면 401")
    void rejectsMissingOrWrongToken() throws Exception {

        mockMvc.perform(post("/api/payments/webhook").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/payments/webhook?token=wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        assertThat(gateway.findCalls).isZero();
    }

    @Test
    @DisplayName("토큰이 맞으면 200")
    void acceptsCorrectToken() throws Exception {

        mockMvc.perform(post("/api/payments/webhook?token=s3cret-token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }
}
