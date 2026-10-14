package com.sp.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 링크·이미지·영상 주소. 업로드한 파일(/uploads/..) 또는 http(s) 주소만 허용한다.
 *
 * javascript:, data:, vbscript:, file: 같은 스킴은 링크를 눌렀을 때 스크립트가 실행되거나
 * 파일이 열리므로 거절한다. 비어 있는 값은 통과한다(필수 여부는 @NotBlank).
 */
@Documented
@Constraint(validatedBy = SafeUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeUrl {

    String message() default "http:// 또는 https:// 로 시작하는 주소만 쓸 수 있어요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
