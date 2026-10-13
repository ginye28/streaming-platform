package com.sp.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비어 있거나, http(s) 주소여야 한다.
 *
 * 사용자가 적은 주소를 링크(&lt;a href&gt;)로 보여 주므로 {@code javascript:} 같은 것을 막는다.
 * 그런 주소가 들어가면 링크를 누른 사람의 브라우저에서 스크립트가 실행된다.
 */
@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {

    String message() default "http:// 또는 https:// 로 시작하는 주소만 쓸 수 있습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
