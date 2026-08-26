package com.sp.api.stream.dto;

import org.springframework.data.domain.Sort;

/**
 * 영상 목록의 정렬 방식.
 *
 * 엔티티 필드 이름(createdAt, viewCount)을 쿼리 문자열로 그대로 받지 않고
 * 열거형으로 좁힌다. 없는 이름이 들어오면 500 이 나고, 내부 구조도 드러나기 때문이다.
 */
public enum StreamSort {

    /** 최신순. 같은 시각에 올라간 영상이 있어도 순서가 흔들리지 않게 id 로 한 번 더 끊는다. */
    LATEST(Sort.by(Sort.Direction.DESC, "createdAt", "id")),

    /** 인기순 = 조회수. 조회수가 같으면 최신 영상을 앞에 둔다. */
    POPULAR(Sort.by(Sort.Direction.DESC, "viewCount", "createdAt", "id"));

    private final Sort sort;

    StreamSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
