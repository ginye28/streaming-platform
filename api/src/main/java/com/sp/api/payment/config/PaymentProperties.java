package com.sp.api.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 유료 구독 결제 설정.
 *
 * 토스페이먼츠 키가 둘 다 없으면 결제는 꺼진 것으로 본다. 이때 유료 구독은 결제 없이 바로 바뀌는
 * 자리표시 동작을 유지한다(개발·데모용). 키를 넣으면 유료 구독은 결제로만 시작된다.
 *
 * 기본값을 @Value 안에 둔 것은 테스트 설정이 main 설정을 통째로 대체해도 값이 비어 있는 채로 뜨게 하려는 것이다.
 */
@Getter
@Component
public class PaymentProperties {

    /** 결제창에 쓰는 클라이언트 키(test_ck_... / live_ck_..., 토스 "API 개별 연동 키"). 브라우저에 내려가도 되는 값이다. */
    @Value("${app.payments.toss.client-key:}")
    private String clientKey;

    /** 결제 승인에 쓰는 시크릿 키. 서버에만 있어야 한다. */
    @Value("${app.payments.toss.secret-key:}")
    private String secretKey;

    @Value("${app.payments.toss.base-url:https://api.tosspayments.com}")
    private String baseUrl;

    /** 유료 구독 한 번에 내는 금액(원). 금액은 항상 서버가 정한다. 브라우저가 보낸 값은 믿지 않는다. */
    @Value("${app.membership.price-krw:4900}")
    private int priceKrw;

    /** 유료 구독 한 번으로 이용할 수 있는 기간(일). */
    @Value("${app.membership.period-days:30}")
    private int periodDays;

    public boolean isEnabled() {
        return clientKey != null && !clientKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
