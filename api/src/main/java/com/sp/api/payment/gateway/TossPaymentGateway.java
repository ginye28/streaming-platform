package com.sp.api.payment.gateway;

import com.sp.api.payment.config.PaymentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 결제 승인 · 조회 API.
 *
 * 인증은 "시크릿 키 + ':'" 를 base64 로 인코딩한 Basic 방식이다. 시크릿 키는 서버 밖으로 나가지 않는다.
 * 승인 요청은 토스가 요구하는 대로 결제창을 연 지 10분 안에 보내야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentGateway implements PaymentGateway {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final PaymentProperties properties;

    @Override
    public GatewayPayment confirm(String paymentKey, String orderId, long amount) {

        return call(() -> client().post()
                .uri("/v1/payments/confirm")
                // 같은 주문을 두 번 승인하지 않도록 주문번호를 멱등 키로 쓴다.
                .header("Idempotency-Key", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                .retrieve()
                .body(MAP_TYPE));
    }

    @Override
    public GatewayPayment find(String paymentKey) {

        return call(() -> client().get()
                .uri("/v1/payments/{paymentKey}", paymentKey)
                .retrieve()
                .body(MAP_TYPE));
    }

    private GatewayPayment call(java.util.function.Supplier<Map<String, Object>> request) {

        try {
            Map<String, Object> body = request.get();

            if (body == null) {
                throw new PaymentGatewayException("TOSS_EMPTY_RESPONSE", "결제사 응답이 비어 있습니다.", true);
            }

            return toPayment(body);

        } catch (RestClientResponseException e) {
            // 4xx 는 결제사가 "이 결제는 안 된다" 고 확정한 것이고, 5xx 는 결제사 쪽 문제라 결과를 알 수 없다.
            boolean serverSide = e.getStatusCode().is5xxServerError();
            Map<String, Object> error = readError(e);

            String code = text(error.get("code"), serverSide ? "TOSS_UNAVAILABLE" : "TOSS_REJECTED");
            String message = text(error.get("message"), "결제 승인에 실패했습니다.");

            log.warn("토스 결제 API 실패 status={} code={}", e.getStatusCode().value(), code);

            throw new PaymentGatewayException(code, message, serverSide);

        } catch (ResourceAccessException e) {
            log.warn("토스 결제 API 에 닿지 못했습니다: {}", e.getMessage());

            throw new PaymentGatewayException(
                    "TOSS_UNAVAILABLE", "결제사에 연결하지 못했습니다. 잠시 뒤 다시 시도해 주세요.", true);
        }
    }

    private RestClient client() {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", basicAuth())
                .build();
    }

    private String basicAuth() {
        String raw = properties.getSecretKey() + ":";
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> readError(RestClientResponseException e) {
        try {
            Map<String, Object> body = e.getResponseBodyAs(MAP_TYPE);
            return body == null ? Map.of() : body;
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private GatewayPayment toPayment(Map<String, Object> body) {

        Object receipt = body.get("receipt");
        String receiptUrl = receipt instanceof Map<?, ?> map ? text(map.get("url"), null) : null;

        Object total = body.get("totalAmount");
        long totalAmount = total instanceof Number number ? number.longValue() : -1L;

        return new GatewayPayment(
                text(body.get("paymentKey"), null),
                text(body.get("orderId"), null),
                text(body.get("status"), null),
                totalAmount,
                text(body.get("method"), null),
                receiptUrl,
                parseTime(text(body.get("approvedAt"), null))
        );
    }

    private static LocalDateTime parseTime(String value) {
        try {
            return value == null ? null : OffsetDateTime.parse(value).toLocalDateTime();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String text(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }
}
