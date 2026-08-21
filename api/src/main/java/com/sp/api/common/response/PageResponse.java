package com.sp.api.common.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
        return of(page, page.getContent());
    }

    /** 페이징 정보는 원본 Page 에서, 내용은 따로 변환한 목록에서 가져온다. */
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public static <T> PageResponse<T> empty(Pageable pageable) {
        return new PageResponse<>(
                List.of(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                0L,
                0,
                true
        );
    }
}
