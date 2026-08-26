package com.sp.api.report.dto;

import com.sp.api.report.entity.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateReportRequest {

    @NotNull
    private Report.TargetType targetType;

    @NotNull
    private Long targetId;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
