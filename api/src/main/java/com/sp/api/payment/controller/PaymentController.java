package com.sp.api.payment.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.payment.dto.CancelPaymentRequest;
import com.sp.api.payment.dto.ConfirmPaymentRequest;
import com.sp.api.payment.dto.EarningsResponse;
import com.sp.api.payment.dto.MembershipOrderResponse;
import com.sp.api.payment.dto.MembershipResultResponse;
import com.sp.api.payment.dto.PaymentConfigResponse;
import com.sp.api.payment.dto.PaymentHistoryResponse;
import com.sp.api.payment.service.EarningsService;
import com.sp.api.payment.service.MembershipService;
import com.sp.api.payment.service.PaymentRefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 유료 구독 결제. 설정 조회만 공개이고 나머지는 로그인이 필요하다.
 */
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final MembershipService membershipService;
    private final PaymentRefundService refundService;
    private final EarningsService earningsService;

    /** 화면이 결제 흐름을 열지 말지 알려면 로그인 전에도 읽을 수 있어야 한다. */
    @GetMapping("/api/payments/config")
    public ResponseEntity<ApiResponse<PaymentConfigResponse>> config() {
        return ResponseEntity.ok(ApiResponse.ok(membershipService.config()));
    }

    /** 1단계 — 이 채널의 유료 구독 주문을 만든다. 돌려받은 값으로 결제창을 연다. */
    @PostMapping("/api/channels/{channelId}/membership/orders")
    public ResponseEntity<ApiResponse<MembershipOrderResponse>> createOrder(
            @PathVariable Long channelId,
            Authentication authentication
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                membershipService.createOrder(channelId, authentication.getName())
        ));
    }

    /** 3단계 — 결제창에서 돌아온 값으로 승인하고 유료 구독을 시작한다. */
    @PostMapping("/api/payments/confirm")
    public ResponseEntity<ApiResponse<MembershipResultResponse>> confirm(
            @Valid @RequestBody ConfirmPaymentRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                membershipService.confirm(authentication.getName(), request)
        ));
    }

    /** 내 결제를 환불한다. 승인 뒤 환불 가능 기간(기본 7일) 안에만 직접 할 수 있다. */
    @PostMapping("/api/payments/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentHistoryResponse>> cancel(
            @PathVariable Long paymentId,
            @Valid @RequestBody(required = false) CancelPaymentRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(refundService.cancelByUser(
                authentication.getName(), paymentId, request == null ? null : request.getReason())));
    }

    /** 채널 주인의 수익 장부. 달별 결제 금액·환불·수수료·정산 예정액. */
    @GetMapping("/api/users/me/earnings")
    public ResponseEntity<ApiResponse<EarningsResponse>> earnings(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(earningsService.earnings(authentication.getName())));
    }

    @GetMapping("/api/users/me/payments")
    public ResponseEntity<ApiResponse<PageResponse<PaymentHistoryResponse>>> history(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                membershipService.history(authentication.getName(), pageable)
        ));
    }
}
