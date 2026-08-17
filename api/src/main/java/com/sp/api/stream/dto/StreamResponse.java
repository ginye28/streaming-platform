package com.sp.api.stream.dto;

import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.entity.StreamStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class StreamResponse {

    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Long viewCount;
    private String streamKey;
    private StreamStatus status;
    private String nickname;
    private LocalDateTime createdAt;

    public StreamResponse(Stream stream) {
        this.id = stream.getId();
        this.title = stream.getTitle();
        this.description = stream.getDescription();
        this.thumbnailUrl = stream.getThumbnailUrl();
        this.videoUrl = stream.getVideoUrl();
        this.viewCount = stream.getViewCount();
        this.streamKey = stream.getStreamKey();
        this.status = stream.getStatus();
        this.nickname = stream.getUser().getNickname();
        this.createdAt = stream.getCreatedAt();
    }
}