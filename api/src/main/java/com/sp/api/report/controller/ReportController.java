package com.sp.api.report.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.report.dto.CreateReportRequest;
import com.sp.api.report.dto.ReportResponse;
import com.sp.api.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 신고 접수. 로그인한 사용자면 누구나 가능하다. 처리는 /api/admin/reports 참고. */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> create(
            @Valid @RequestBody CreateReportRequest request,
            Authentication authentication
    ) {

        ReportResponse response = reportService.create(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
