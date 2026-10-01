package com.sp.api.payment.repository;

import com.sp.api.payment.entity.PaymentStatus;

/** 승인된 달과 상태별 결제 건수·금액. 수익 장부를 집계할 때 쓰는 최소한의 값. */
public record MonthlyTotal(Integer year, Integer month, PaymentStatus status, Long count, Long amount) {
}
