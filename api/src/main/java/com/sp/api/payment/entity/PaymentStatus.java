package com.sp.api.payment.entity;

public enum PaymentStatus {
    /** 주문을 만들었고 아직 승인 전. */
    READY,
    /** 결제 승인까지 끝났다. */
    DONE,
    /** 승인에 실패했다. 사유는 failureCode · failureMessage 에 남는다. */
    FAILED
}
