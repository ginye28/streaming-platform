package com.sp.api.stream.dto;

import com.sp.api.stream.entity.Stream;
import lombok.Getter;

@Getter
public class StreamResponse {

    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Long viewCount;
    private String nickname;

    public StreamResponse(Stream stream) {
        this.id = stream.getId();
        this.title = stream.getTitle();
        this.description = stream.getDescription();
        this.thumbnailUrl = stream.getThumbnailUrl();
        this.videoUrl = stream.getVideoUrl();
        this.viewCount = stream.getViewCount();
        this.nickname = stream.getUser().getNickname();
    }
}