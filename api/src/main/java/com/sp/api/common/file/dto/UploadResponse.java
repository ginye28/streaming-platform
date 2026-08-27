package com.sp.api.common.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadResponse {

    private String url;

    /** 영상이면 첫 장면으로 만든 썸네일. 못 만들었거나 영상이 아니면 null. */
    private String thumbnailUrl;
}
