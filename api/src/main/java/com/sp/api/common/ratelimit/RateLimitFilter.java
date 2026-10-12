package com.sp.api.common.ratelimit;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.security.SecurityEventRecorder;
import com.sp.api.common.web.ClientIp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 접속 IP 별로 요청 속도를 제한한다. 넘치면 429 와 Retry-After 로 돌려보낸다.
 *
 * 요청 종류마다 따로 센다(로그인은 엄격하게, 조회는 넉넉하게). 종류는 아래 {@link #classify} 가 정한다.
 * SecurityConfig 가 직접 만들어 보안 필터 체인에 넣는다 — 빈으로 등록하면 서블릿 필터로도 한 번 더 걸린다.
 */
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration MINUTE = Duration.ofMinutes(1);

    private final RateLimiter limiter;
    private final RateLimitProperties properties;
    private final SecurityEventRecorder recorder;
    private final ObjectMapper objectMapper;

    enum Kind {
        AUTH, WRITE, READ, UPLOAD, CONNECT
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Kind kind = properties.isEnabled() ? classify(request.getMethod(), request.getRequestURI()) : null;

        if (kind == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ClientIp.of(request);

        RateLimiter.Decision decision = limiter.tryConsume(
                kind + ":" + ip, capacityOf(kind), MINUTE);

        if (decision.allowed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
            filterChain.doFilter(request, response);
            return;
        }

        // 한 사람이 계속 두드려도 로그와 목록이 그 사람으로 가득 차지 않도록, 같은 사람·종류는 1분에 한 번만 남긴다.
        boolean store = limiter.tryConsume("report:" + kind + ":" + ip, 1, MINUTE).allowed();

        recorder.record(SecurityEventRecorder.Type.RATE_LIMITED, ip, request.getRequestURI(),
                kind + " 한도 초과", store);

        reject(response, decision.retryAfterSeconds());
    }

    /** @return 제한 대상이 아니면 null */
    static Kind classify(String method, String path) {

        if ("OPTIONS".equals(method)) {
            return null;
        }

        // 영상 조각은 시청자 한 명이 2초마다 받는다. 내용은 공개 파일이라 제한하지 않는다.
        // 내부 콜백(nginx)과 웹훅(토스)은 늘 같은 곳에서 오고, 각자 비밀 값으로 막혀 있다.
        if (path.startsWith("/live-hls/") || path.startsWith("/uploads/")
                || path.startsWith("/api/internal/") || path.equals("/api/payments/webhook")
                || path.startsWith("/swagger") || path.startsWith("/v3/api-docs")) {
            return null;
        }

        if (path.startsWith("/ws") || path.startsWith("/ingest")) {
            return Kind.CONNECT;
        }

        if (!path.startsWith("/api/")) {
            return null;
        }

        boolean read = "GET".equals(method) || "HEAD".equals(method);

        if (path.startsWith("/api/auth/") && !read) {
            return Kind.AUTH;
        }

        if (path.startsWith("/api/files/") && !read) {
            return Kind.UPLOAD;
        }

        return read ? Kind.READ : Kind.WRITE;
    }

    private int capacityOf(Kind kind) {
        return switch (kind) {
            case AUTH -> properties.getAuthPerMinute();
            case WRITE -> properties.getWritePerMinute();
            case READ -> properties.getReadPerMinute();
            case UPLOAD -> properties.getUploadPerMinute();
            case CONNECT -> properties.getConnectPerMinute();
        };
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.error("요청이 너무 많아요. " + retryAfterSeconds + "초 뒤에 다시 시도해 주세요.")
        );
    }
}
