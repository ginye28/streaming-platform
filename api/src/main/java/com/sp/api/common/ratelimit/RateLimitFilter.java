package com.sp.api.common.ratelimit;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.security.ClientIp;
import com.sp.api.common.security.SecurityEventRecorder;
import com.sp.api.common.security.SlidingWindowRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * 접속 IP 별로 일반 요청의 속도를 제한한다. 넘치면 429 와 Retry-After 로 돌려보낸다.
 *
 * 요청 종류마다 따로 센다(변경은 엄격하게, 조회는 넉넉하게). 종류는 아래 {@link #classify} 가 정한다.
 * 로그인·가입·토큰 갱신은 여기서 세지 않는다 — {@code AuthRateLimitFilter} 가 더 엄격한 한도로 따로 센다.
 * SecurityConfig 가 직접 만들어 보안 필터 체인에 넣는다. 빈으로 등록하면 서블릿 필터로도 한 번 더 걸린다.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long MINUTE_MILLIS = 60_000L;

    private final RateLimitProperties properties;
    private final SecurityEventRecorder recorder;
    private final ObjectMapper objectMapper;

    private final Map<Kind, SlidingWindowRateLimiter> limiters = new EnumMap<>(Kind.class);

    /** 같은 사람·종류는 1분에 한 번만 기록해 로그와 목록이 그 한 사람으로 가득 차지 않게 한다. */
    private final SlidingWindowRateLimiter reportLimiter = new SlidingWindowRateLimiter(1, MINUTE_MILLIS);

    enum Kind {
        WRITE, READ, UPLOAD, CONNECT
    }

    public RateLimitFilter(RateLimitProperties properties, SecurityEventRecorder recorder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.recorder = recorder;
        this.objectMapper = objectMapper;

        limiters.put(Kind.WRITE, new SlidingWindowRateLimiter(properties.getWritePerMinute(), MINUTE_MILLIS));
        limiters.put(Kind.READ, new SlidingWindowRateLimiter(properties.getReadPerMinute(), MINUTE_MILLIS));
        limiters.put(Kind.UPLOAD, new SlidingWindowRateLimiter(properties.getUploadPerMinute(), MINUTE_MILLIS));
        limiters.put(Kind.CONNECT, new SlidingWindowRateLimiter(properties.getConnectPerMinute(), MINUTE_MILLIS));
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
        SlidingWindowRateLimiter limiter = limiters.get(kind);

        if (limiter.tryAcquire(ip)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean store = reportLimiter.tryAcquire(kind + ":" + ip);

        recorder.record(SecurityEventRecorder.Type.RATE_LIMITED, ip, request.getRequestURI(),
                kind + " 한도 초과", store);

        reject(response, Math.max(1, limiter.retryAfterSeconds(ip)));
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

        if (path.startsWith("/ws") || path.startsWith("/ingest") || path.startsWith("/watch")) {
            return Kind.CONNECT;
        }

        // 인증 경로는 AuthRateLimitFilter 와 AuthService 가 따로 제한한다.
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/")) {
            return null;
        }

        boolean read = "GET".equals(method) || "HEAD".equals(method);

        if (path.startsWith("/api/files/") && !read) {
            return Kind.UPLOAD;
        }

        return read ? Kind.READ : Kind.WRITE;
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
