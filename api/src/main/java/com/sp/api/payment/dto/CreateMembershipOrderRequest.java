package com.sp.api.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유료 구독 주문 요청. 본문을 아예 안 보내거나 months 를 비우면 기본 상품(1개월)이다.
 * 금액은 받지 않는다. 서버가 상품에서 정한다.
 */
@Getter
@NoArgsConstructor
public class CreateMembershipOrderRequest {

    /** 구독 기간(달). 설정에 있는 상품만 된다. 기본 1, 3, 6, 12. */
    private Integer months;
}
