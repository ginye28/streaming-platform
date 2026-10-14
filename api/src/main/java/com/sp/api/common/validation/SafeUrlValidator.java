package com.sp.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 업로드 경로(/uploads/..) 또는 http(s) 주소. http(s) 판정은 {@link HttpUrlValidator} 와 같은 구현을 쓴다.
 * 외부 링크만 받는 칸(크레딧 링크)은 {@link HttpUrl} 을 쓴다.
 */
public class SafeUrlValidator implements ConstraintValidator<SafeUrl, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        String raw = value.toString();

        if (raw.isBlank()) {
            return true;
        }

        // 업로드 API 가 돌려준 상대 경로. "//evil.com" 은 다른 사이트를 가리키므로 뺀다.
        if (raw.startsWith("/")) {
            return !raw.startsWith("//") && !raw.contains("\\") && noControlOrSpace(raw);
        }

        return HttpUrlValidator.isHttpUrl(raw);
    }

    private static boolean noControlOrSpace(String value) {

        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i)) || Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
