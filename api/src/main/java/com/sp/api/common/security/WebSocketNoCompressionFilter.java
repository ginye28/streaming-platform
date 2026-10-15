package com.sp.api.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 영상 WebSocket(/ingest, /watch)의 압축 확장(permessage-deflate)을 협상하지 못하게 요청 헤더에서 지운다.
 *
 * 영상 조각은 이미 압축돼 있어 한 번 더 압축해도 줄지 않는데, 브라우저는 압축을 항상 제안하고 Tomcat 은 수락해서
 * 메시지마다 Deflater 를 돌린다. 0.1 CPU 서버에서 시청자 13명일 때 스레드 덤프 대부분이 이 압축이었다.
 * (Tomcat 은 서버 쪽 설정으로 확장을 끌 수 없어서 제안 자체를 지운다.) 채팅(/ws)은 작은 글이라 그대로 둔다.
 */
public class WebSocketNoCompressionFilter extends OncePerRequestFilter {

    private static final String HEADER = "Sec-WebSocket-Extensions";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/ingest") || path.startsWith("/watch"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(new HttpServletRequestWrapper(request) {

            @Override
            public String getHeader(String name) {
                return HEADER.equalsIgnoreCase(name) ? null : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return HEADER.equalsIgnoreCase(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                List<String> names = Collections.list(super.getHeaderNames());
                names.removeIf(HEADER::equalsIgnoreCase);
                return Collections.enumeration(names);
            }
        }, response);
    }
}
