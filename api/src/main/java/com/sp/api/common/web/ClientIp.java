package com.sp.api.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청을 보낸 사람의 접속 IP.
 *
 * Render·nginx 처럼 앞에 프록시가 있으면 서버가 보는 접속 주소는 프록시 하나뿐이라,
 * 프록시가 붙여 주는 X-Forwarded-For 의 첫 번째 값을 쓴다.
 * 이 값은 클라이언트가 직접 적어 보낼 수도 있어서 "누구인지 증명하는" 용도가 아니라
 * 조회수 중복·요청 속도 제한처럼 "대충 구분하는" 용도로만 쓴다.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }

        String first = forwarded.split(",")[0].trim();

        // 헤더에 쓰레기가 들어와 키가 무한히 늘어나지 않게 길이를 막는다.
        return first.isEmpty() || first.length() > 45 ? request.getRemoteAddr() : first;
    }
}
