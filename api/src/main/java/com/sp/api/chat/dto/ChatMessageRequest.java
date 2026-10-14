package com.sp.api.chat.dto;

import com.sp.api.common.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRequest {

    @NotBlank
    @Size(max = 500)
    @SafeText
    private String content;
}
