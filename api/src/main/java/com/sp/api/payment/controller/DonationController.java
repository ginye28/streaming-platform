package com.sp.api.payment.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.payment.dto.DonationOrderRequest;
import com.sp.api.payment.dto.MembershipOrderResponse;
import com.sp.api.payment.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 방송 후원. 주문만 여기서 만들고, 승인은 구독과 같은 POST /api/payments/confirm 이 한다. */
@RestController
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    /** 1단계 — 이 방송의 후원 주문을 만든다. 돌려받은 값으로 결제창을 연다. */
    @PostMapping("/api/lives/{liveId}/donations/orders")
    public ResponseEntity<ApiResponse<MembershipOrderResponse>> createOrder(
            @PathVariable Long liveId,
            @Valid @RequestBody DonationOrderRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                donationService.createOrder(liveId, authentication.getName(), request)
        ));
    }
}
