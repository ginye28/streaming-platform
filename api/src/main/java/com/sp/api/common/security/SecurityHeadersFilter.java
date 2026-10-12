package com.sp.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 응답에 브라우저 방어 헤더를 붙인다.
 *
 * - Content-Security-Policy: 이 서버의 응답(JSON·업로드한 파일)은 어떤 스크립트도 실행할 일이 없다.
 *   그래서 전부 막아 두면, 업로드한 파일을 주소창으로 직접 열어도 스크립트가 돌 수 없다.
 *   Swagger 화면만 스크립트를 쓰므로 뺀다.
 * - X-Content-Type-Options: 파일 확장자·내용을 보고 브라우저가 다른 종류로 추측하지 못하게 한다.
 * - Referrer-Policy, Permissions-Policy: 주소·카메라·마이크 같은 정보가 새거나 쓰이지 않게 한다.
 *
 * 화면(웹)의 헤더는 web/vercel.json 과 web/nginx.conf 에서 따로 준다.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String API_CSP =
            "default-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");

        if (!isSwagger(request.getRequestURI())) {
            response.setHeader("Content-Security-Policy", API_CSP);
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isSwagger(String path) {
        return path.startsWith("/swagger") || path.startsWith("/v3/api-docs");
    }
}
