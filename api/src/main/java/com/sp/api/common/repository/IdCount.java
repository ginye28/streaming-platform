package com.sp.api.common.repository;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "id 별 개수" 집계 결과. 목록 조회 시 건별로 count 쿼리를 날리지 않기 위해 쓴다.
 */
public record IdCount(Long id, long count) {

    public static Map<Long, Long> toMap(Collection<IdCount> counts) {
        return counts.stream()
                .collect(Collectors.toMap(IdCount::id, IdCount::count, (a, b) -> a));
    }
}
