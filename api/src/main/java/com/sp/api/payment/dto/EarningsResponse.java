package com.sp.api.payment.dto;

import java.util.List;

/**
 * 채널 주인이 보는 수익 장부.
 *
 * 장부일 뿐이다. 실제 송금은 하지 않는다. 결제 금액은 서비스(가맹점)로 들어오므로
 * 여기 적힌 "정산 예정액" 을 채널 주인에게 보내는 일은 따로 해야 한다.
 */
public record EarningsResponse(
        /** 서비스 수수료율(%). 설정값이다. */
        int feePercent,
        Totals total,
        /** 승인된 달 기준, 최근 달이 위. */
        List<Month> months
) {

    /**
     * @param gross    결제 금액 합계(환불된 것 포함)
     * @param refunded 환불된 금액 합계
     * @param fee      (gross - refunded) 에 수수료율을 곱한 금액
     * @param net      정산 예정액 = gross - refunded - fee
     * @param count    결제 건수(환불된 것 포함)
     */
    public record Totals(long gross, long refunded, long fee, long net, long count) {
    }

    public record Month(String month, long gross, long refunded, long fee, long net, long count) {
    }
}
