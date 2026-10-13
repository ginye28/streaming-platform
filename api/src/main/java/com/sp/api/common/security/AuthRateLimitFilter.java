package com.sp.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 로그인·가입·토큰 갱신을 같은 곳(IP)에서 너무 자주 부르지 못하게 한다.
 * 비밀번호를 무작위로 대입하거나 계정을 대량으로 만드는 것을 늦추려는 장치다.
 *
 * 계정별 로그인 실패 제한은 {@code AuthService} 가 따로 한다(여러 IP 에서 한 계정을 노리는 경우).
 */
@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 10 * 60 * 1000L;

    private final boolean enabled;

    /** 경로별 한도. 학교 같은 공용 IP 에서 여럿이 쓸 수 있어서 넉넉하게 둔다. */
    private final Map<String, SlidingWindowRateLimiter> limiters;

    public AuthRateLimitFilter(
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.login-per-10min:60}") int loginMax,
            @Value("${app.security.rate-limit.signup-per-10min:20}") int signupMax,
            @Value("${app.security.rate-limit.refresh-per-10min:120}") int refreshMax
    ) {
        this.enabled = enabled;
        this.limiters = Map.of(
                "/api/auth/login", new SlidingWindowRateLimiter(loginMax, WINDOW_MILLIS),
                "/api/auth/signup", new SlidingWindowRateLimiter(signupMax, WINDOW_MILLIS),
                "/api/auth/refresh", new SlidingWindowRateLimiter(refreshMax, WINDOW_MILLIS)
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled
                || !"POST".equals(request.getMethod())
                || !limiters.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        SlidingWindowRateLimiter limiter = limiters.get(request.getRequestURI());
        String key = ClientIp.of(request);

        if (!limiter.tryAcquire(key)) {

            long retryAfter = Math.max(1, limiter.retryAfterSeconds(key));

            log.warn("요청이 너무 잦아 막았습니다: {} {}", request.getRequestURI(), key);

            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json;charset=UTF-8");
            response.getOutputStream().write(
                    ("{\"message\":\"요청이 너무 많습니다. " + retryAfter + "초 뒤에 다시 시도해 주세요.\",\"success\":false}")
                            .getBytes(StandardCharsets.UTF_8)
            );
            return;
        }

        chain.doFilter(request, response);
    }
}
