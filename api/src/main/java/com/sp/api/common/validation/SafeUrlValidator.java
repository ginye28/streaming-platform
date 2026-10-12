package com.sp.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class SafeUrlValidator implements ConstraintValidator<SafeUrl, CharSequence> {

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        String raw = value.toString();

        // trim() 은 끝의 제어 문자(NUL 등)도 지워 버리므로, 다듬기 전의 원본으로 먼저 본다.
        if (SafeTextValidator.hasForbiddenCharacter(raw)) {
            return false;
        }

        String url = raw.strip();

        if (url.isEmpty()) {
            return true;
        }

        if (url.contains("\\") || url.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }

        // 업로드 API 가 돌려준 상대 경로. "//evil.com" 은 다른 사이트를 가리키므로 뺀다.
        if (url.startsWith("/")) {
            return !url.startsWith("//");
        }

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);

            return (scheme.equals("http") || scheme.equals("https")) && uri.getHost() != null;

        } catch (URISyntaxException e) {
            return false;
        }
    }
}
