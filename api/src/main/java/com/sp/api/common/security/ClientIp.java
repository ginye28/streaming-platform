package com.sp.api.common.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청을 보낸 쪽의 IP.
 *
 * 운영 서버(Render)는 Cloudflare 뒤에 있고, Cloudflare 는 {@code CF-Connecting-IP} 를 항상 직접 채운다
 * (클라이언트가 보낸 값은 덮어쓴다). 그래서 이 헤더가 있으면 그것을 쓴다.
 * 없으면(로컬 개발, 직접 접속) 소켓 주소를 쓴다. {@code X-Forwarded-For} 는 클라이언트가 앞에 아무 값이나
 * 붙일 수 있어서 믿지 않는다.
 */
final class ClientIp {

    private ClientIp() {
    }

    static String of(HttpServletRequest request) {

        String cloudflare = request.getHeader("CF-Connecting-IP");

        if (cloudflare != null && !cloudflare.isBlank() && cloudflare.length() <= 64) {
            return cloudflare.trim();
        }

        return request.getRemoteAddr();
    }
}
