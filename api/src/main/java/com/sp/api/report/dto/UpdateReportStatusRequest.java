package com.sp.api.report.dto;

import com.sp.api.report.entity.Report;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateReportStatusRequest {

    @NotNull
    private Report.Status status;
}
