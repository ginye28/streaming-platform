package com.sp.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 토스 키가 없을 때(기본 설정). 결제는 꺼지고, 유료 전환은 결제 없는 자리표시로 남는다.
 * 키를 넣기 전에도 서비스가 멈추지 않는지, 그리고 시크릿 키가 어디에도 새지 않는지를 본다.
 */
class PaymentDisabledIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("키가 없으면 결제 설정은 꺼짐이고 클라이언트 키는 내려가지 않는다")
    void configIsOffWithoutKeys() throws Exception {

        mockMvc.perform(get("/api/payments/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.clientKey").doesNotExist());
    }

    @Test
    @DisplayName("키가 없으면 주문도 승인도 503 이다")
    void ordersAndConfirmAreUnavailable() throws Exception {

        String owner = signupAndLogin("off-owner@test.com", "꺼짐주인");
        String fan = signupAndLogin("off-fan@test.com", "꺼짐팬");

        mockMvc.perform(post("/api/channels/" + myUserId(owner) + "/membership/orders")
                        .header("Authorization", "Bearer " + fan))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", "Bearer " + fan)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentKey":"k","orderId":"sub-1-x","amount":4900}"""))
                .andExpect(status().isServiceUnavailable());
    }
}
