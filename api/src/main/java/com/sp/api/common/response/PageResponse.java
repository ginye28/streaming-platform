package com.sp.api.common.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Spring Data의 Page를 그대로 직렬화하면 응답 형태가 버전에 따라 흔들리므로
 * 필요한 필드만 담은 안정적인 계약을 노출한다.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
