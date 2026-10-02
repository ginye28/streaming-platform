package com.sp.api.payment.entity;

/** 결제가 무엇을 위한 것인지. 같은 결제 흐름(주문 → 결제창 → 승인)을 두 가지가 나눠 쓴다. */
public enum PaymentKind {
    /** 채널 유료 구독(이용권). 승인되면 유료 기간이 늘어난다. */
    SUBSCRIPTION,
    /** 방송 후원(슈퍼챗). 승인되면 채팅에 후원 메시지가 올라간다. */
    DONATION
}
