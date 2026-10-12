package com.sp.api.payment.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

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

    /** 유료 구독 한 달(1개월)의 기간(일). 여러 달 상품은 이 값에 달 수를 곱한다. */
    @Value("${app.membership.period-days:30}")
    private int periodDays;

    /** 고를 수 있는 구독 기간(달). 처음 값이 기본 상품이다. */
    @Value("${app.membership.plan-months:1,3,6,12}")
    private List<Integer> planMonths;

    /** planMonths 와 같은 순서의 할인율(%). 오래 구독할수록 한 달 값이 싸진다. */
    @Value("${app.membership.plan-discounts:0,5,10,15}")
    private List<Integer> planDiscounts;

    /**
     * 웹훅 주소에 붙이는 비밀 값. 토스 개발자센터에 웹훅 주소를 등록할 때 ?token=... 으로 함께 적는다.
     * 비어 있으면 검사하지 않는다(그래도 본문은 믿지 않고 토스에 다시 조회한다).
     */
    @Value("${app.payments.toss.webhook-token:}")
    private String webhookToken;

    /** 사용자가 직접 환불할 수 있는 기간(일). 승인된 날부터 센다. */
    @Value("${app.membership.refund-window-days:7}")
    private int refundWindowDays;

    /** 채널 주인 수익 장부에서 떼는 서비스 수수료율(%). */
    @Value("${app.membership.platform-fee-percent:0}")
    private int platformFeePercent;

    /** 유료 구독이 끝나기 며칠 전부터 알릴지. */
    @Value("${app.membership.expiry-notice-days:3}")
    private int expiryNoticeDays;

    /** 후원으로 보낼 수 있는 금액(원). 이 목록에 없는 금액은 서버가 주문을 만들어 주지 않는다. */
    @Value("${app.donation.amounts:1000,3000,5000,10000,30000,50000}")
    private List<Integer> donationAmounts;

    /** 구독 기간 상품. 금액은 한 달 금액 × 달 수에서 할인율을 빼고 100원 단위로 내린 값이다. */
    public record Plan(int months, int days, int amountKrw, int discountPercent) {
    }

    public List<Plan> plans() {

        List<Plan> plans = new java.util.ArrayList<>();

        for (int i = 0; i < planMonths.size(); i++) {

            int months = planMonths.get(i);
            int discount = i < planDiscounts.size() ? planDiscounts.get(i) : 0;

            if (months < 1) {
                continue;
            }

            long gross = (long) priceKrw * months;
            long discounted = gross * (100 - discount) / 100;
            // 한 달 상품(할인 0)은 설정한 금액 그대로, 나머지는 100원 단위로 내린다.
            int amount = (int) (discount == 0 ? discounted : (discounted / 100) * 100);

            plans.add(new Plan(months, months * periodDays, amount, discount));
        }

        if (plans.isEmpty()) {
            plans.add(new Plan(1, periodDays, priceKrw, 0));
        }

        return plans;
    }

    /** 달 수에 맞는 상품. 없는 상품이면 null. months 가 null 이면 기본(첫) 상품. */
    public Plan planOf(Integer months) {

        List<Plan> plans = plans();

        if (months == null) {
            return plans.get(0);
        }

        return plans.stream().filter(plan -> plan.months() == months).findFirst().orElse(null);
    }

    public boolean isEnabled() {
        return clientKey != null && !clientKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
