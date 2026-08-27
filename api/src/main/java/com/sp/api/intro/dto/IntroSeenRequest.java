package com.sp.api.intro.dto;

import com.sp.api.intro.entity.IntroImpression;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IntroSeenRequest {

    @NotNull(message = "무엇을 했는지 알려 주세요. (SKIP · WATCHED · PASS)")
    private IntroImpression.Action action;
}
