package com.sp.api.stream.dto;

import com.sp.api.stream.entity.Stream;

import java.time.LocalDateTime;

public record StreamResponse(
        Long id,
        String title,
        String description,
        String thumbnailUrl,
        String videoUrl,
        Long viewCount,
        long likeCount,
        long commentCount,
        /** 요청한 사용자가 좋아요를 눌렀는지. 비로그인이면 항상 false. */
        boolean likedByMe,
        Long userId,
        String nickname,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt
) {

    public static StreamResponse of(Stream stream, long likeCount, long commentCount, boolean likedByMe) {
        return new StreamResponse(
                stream.getId(),
                stream.getTitle(),
                stream.getDescription(),
                stream.getThumbnailUrl(),
                stream.getVideoUrl(),
                stream.getViewCount(),
                likeCount,
                commentCount,
                likedByMe,
                stream.getUser().getId(),
                stream.getUser().getNickname(),
                stream.getCategory() != null ? stream.getCategory().getId() : null,
                stream.getCategory() != null ? stream.getCategory().getName() : null,
                stream.getCreatedAt()
        );
    }

    /** 방금 만든 영상처럼 집계가 0 인 것이 자명한 경우. */
    public static StreamResponse ofNew(Stream stream) {
        return of(stream, 0L, 0L, false);
    }
}
