package com.sp.api.report.service;

import com.sp.api.comment.repository.CommentRepository;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.live.repository.LiveStreamRepository;
import com.sp.api.report.dto.CreateReportRequest;
import com.sp.api.report.dto.ReportResponse;
import com.sp.api.report.dto.UpdateReportStatusRequest;
import com.sp.api.report.entity.Report;
import com.sp.api.report.repository.ReportRepository;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final StreamRepository streamRepository;
    private final LiveStreamRepository liveStreamRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ReportResponse create(CreateReportRequest request, String email) {

        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (!targetExists(request.getTargetType(), request.getTargetId())) {
            throw new NotFoundException("신고 대상을 찾을 수 없습니다.");
        }

        Report report = new Report(
                reporter, request.getTargetType(), request.getTargetId(), request.getReason());

        return ReportResponse.from(reportRepository.save(report));
    }

    /** 관리자 전용. 인가는 SecurityConfig 에서 ADMIN 으로 제한한다. */
    public PageResponse<ReportResponse> findAll(Report.Status status, Pageable pageable) {

        Page<Report> page = status != null
                ? reportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : reportRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponse.from(page.map(ReportResponse::from));
    }

    /** 관리자 전용. 인가는 SecurityConfig 에서 ADMIN 으로 제한한다. */
    @Transactional
    public ReportResponse updateStatus(Long id, UpdateReportStatusRequest request) {

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다."));

        report.updateStatus(request.getStatus());

        return ReportResponse.from(report);
    }

    private boolean targetExists(Report.TargetType targetType, Long targetId) {
        return switch (targetType) {
            case STREAM -> streamRepository.existsById(targetId);
            case LIVE_STREAM -> liveStreamRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            case USER -> userRepository.existsById(targetId);
        };
    }
}
