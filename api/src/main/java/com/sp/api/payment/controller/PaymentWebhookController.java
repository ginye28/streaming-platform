package com.sp.api.payment.controller;

import com.sp.api.common.exception.UnauthorizedException;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.payment.config.PaymentProperties;
import com.sp.api.payment.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * 토스페이먼츠 웹훅 수신. 토스 개발자센터의 웹훅 주소에
 * <code>https://{API 주소}/api/payments/webhook?token={TOSS_WEBHOOK_TOKEN}</code> 을 등록한다.
 *
 * 인증이 없는 공개 주소라서 토큰으로 한 겹 막고(설정했을 때), 본문은 믿지 않고 토스에 다시 조회한다.
 * 토스는 200 이 아니면 다시 보내므로, 우리가 처리할 필요가 없는 이벤트도 200 으로 돌려준다.
 */
@RestController
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;
    private final PaymentProperties properties;

    @PostMapping("/api/payments/webhook")
    public ResponseEntity<ApiResponse<Void>> receive(
            @RequestParam(required = false) String token,
            @RequestBody(required = false) Map<String, Object> payload
    ) {

        verifyToken(token);

        webhookService.handle(payload);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /** 토큰을 설정하지 않았다면 검사하지 않는다. 설정했다면 시간이 걸리는 차이로 값을 추측하지 못하게 비교한다. */
    private void verifyToken(String token) {

        String expected = properties.getWebhookToken();

        if (expected == null || expected.isBlank()) {
            return;
        }

        boolean same = token != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));

        if (!same) {
            throw new UnauthorizedException("웹훅 토큰이 올바르지 않습니다.");
        }
    }
}
