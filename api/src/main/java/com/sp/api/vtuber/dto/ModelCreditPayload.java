package com.sp.api.vtuber.dto;

import com.sp.api.vtuber.entity.ModelCredit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModelCreditPayload {

    @NotNull(message = "맡은 일을 골라 주세요.")
    private ModelCredit.Role role;

    @NotBlank(message = "이름을 적어 주세요.")
    @Size(max = 60, message = "이름은 60자 이하여야 합니다.")
    private String name;

    @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
    private String link;
}
