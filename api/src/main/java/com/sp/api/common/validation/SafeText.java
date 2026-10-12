package com.sp.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 사람이 읽는 글(채팅·댓글·제목·소개 등)에 HTML 태그·스크립트·눈에 안 보이는 제어 문자가 섞여 있으면 거절한다.
 *
 * 화면(React)은 글을 항상 "글자"로만 그려서 태그가 있어도 실행되지 않지만,
 * 이 값이 다른 곳(이메일, 관리자 도구, 다른 클라이언트)에서 HTML 로 읽힐 수 있으므로 서버에서도 막는다.
 * 저장할 때 글자를 바꿔 치우지(escape) 않고 거절하는 이유는, 바꿔 저장하면 화면에서 두 번 바뀌어 보이기 때문이다.
 *
 * 비어 있는 값(null·공백)은 통과한다. 필수 여부는 @NotBlank 가 따로 본다.
 * 허용하는 것: "I &lt;3 you", "1 &lt; 2 &gt; 0" 처럼 태그 모양이 아닌 부등호와 이모지, 줄바꿈, 탭,
 * 그리고 "javascript: 어렵다" 같은 평범한 말(링크로 실행될 수 있는 주소 칸은 @SafeUrl 이 막는다).
 */
@Documented
@Constraint(validatedBy = SafeTextValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeText {

    String message() default "HTML 태그나 스크립트, 보이지 않는 제어 문자는 쓸 수 없어요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
