package com.sp.api.payment.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.payment.dto.AdminPaymentResponse;
import com.sp.api.payment.dto.CancelPaymentRequest;
import com.sp.api.payment.entity.PaymentStatus;
import com.sp.api.payment.service.PaymentRefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 결제 관리. 인가는 SecurityConfig 에서 ADMIN 으로 제한한다. */
@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRefundService refundService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminPaymentResponse>>> findAll(
            @RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return ResponseEntity.ok(ApiResponse.ok(refundService.findForAdmin(status, pageable)));
    }

    /** 어느 결제든 환불한다. 환불 가능 기간을 따지지 않고, 사유가 필요하다. */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<AdminPaymentResponse>> cancel(
            @PathVariable Long paymentId,
            @Valid @RequestBody CancelPaymentRequest request
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                refundService.cancelByAdmin(paymentId, request.getReason())
        ));
    }
}
