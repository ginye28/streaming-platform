package com.sp.api.live.dto;

import com.sp.api.live.entity.LiveSetting;

public record LiveSettingResponse(
        String title,
        String description,
        String thumbnailUrl,
        String audience,
        String chatAudience,
        int slowModeSeconds
) {

    public static LiveSettingResponse from(LiveSetting setting) {
        return new LiveSettingResponse(
                setting.getTitle(),
                setting.getDescription(),
                setting.getThumbnailUrl(),
                setting.getAudience().name(),
                setting.getChatAudience().name(),
                setting.getSlowModeSeconds()
        );
    }
}
