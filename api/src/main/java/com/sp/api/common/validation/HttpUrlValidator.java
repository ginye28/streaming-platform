package com.sp.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.util.Locale;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isBlank()) {
            return true;
        }

        return isHttpUrl(value);
    }

    /** http 또는 https 이고 호스트가 있는 주소인지. 공백·제어 문자가 섞인 값은 거절한다. */
    public static boolean isHttpUrl(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        // 브라우저는 주소 앞뒤의 공백·제어 문자를 무시하므로 " javascript:..." 같은 우회를 여기서 막는다.
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i)) || Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();

            if (scheme == null) {
                return false;
            }

            String lower = scheme.toLowerCase(Locale.ROOT);

            return (lower.equals("http") || lower.equals("https"))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();

        } catch (Exception e) {
            return false;
        }
    }
}
