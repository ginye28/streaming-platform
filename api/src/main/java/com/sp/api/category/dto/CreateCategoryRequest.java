package com.sp.api.category.dto;

import com.sp.api.common.validation.SafeText;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCategoryRequest {

    @NotBlank
    @Size(max = 30)
    @SafeText
    private String name;
}
