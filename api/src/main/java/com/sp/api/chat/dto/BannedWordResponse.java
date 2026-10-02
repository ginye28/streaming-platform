package com.sp.api.chat.dto;

import com.sp.api.chat.moderation.ChannelBannedWord;

public record BannedWordResponse(Long id, String word) {

    public static BannedWordResponse from(ChannelBannedWord word) {
        return new BannedWordResponse(word.getId(), word.getWord());
    }
}
