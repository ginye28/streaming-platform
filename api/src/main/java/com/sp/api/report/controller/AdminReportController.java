package com.sp.api.report.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.report.dto.ReportResponse;
import com.sp.api.report.dto.UpdateReportStatusRequest;
import com.sp.api.report.entity.Report;
import com.sp.api.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 신고 처리. 인가는 SecurityConfig 에서 ADMIN 권한으로 제한한다. */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> findAll(
            @RequestParam(required = false) Report.Status status,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return ResponseEntity.ok(ApiResponse.ok(reportService.findAll(status, pageable)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReportStatusRequest request
    ) {

        return ResponseEntity.ok(ApiResponse.ok(reportService.updateStatus(id, request)));
    }
}
