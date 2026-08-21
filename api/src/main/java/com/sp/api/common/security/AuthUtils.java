package com.sp.api.common.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * 공개 엔드포인트에서는 Authentication 이 null 이거나 익명 토큰일 수 있다.
 * 로그인한 경우에만 이메일을, 아니면 null 을 돌려준다.
 */
public final class AuthUtils {

    private AuthUtils() {
    }

    public static String emailOrNull(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return authentication.getName();
    }
}
