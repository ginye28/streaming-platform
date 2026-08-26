package com.sp.api.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 조회수 중복을 가릴 때 쓰는 "이 사람" 식별자.
 * 로그인했으면 계정으로, 아니면 접속 IP 로 구분한다.
 *
 * IP 는 공유될 수 있어(같은 공유기, 학원 등) 완벽한 구분은 아니다.
 * 다만 새로고침만으로 조회수가 부풀지 않게 막는 데는 충분하다.
 */
public final class ViewerKey {

    private ViewerKey() {
    }

    public static String of(String email, HttpServletRequest request) {
        return email != null ? "user:" + email : "ip:" + clientIp(request);
    }

    /** nginx 를 앞에 두면 실제 접속 IP 가 X-Forwarded-For 의 첫 번째 값으로 온다. */
    private static String clientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }

        return forwarded.split(",")[0].trim();
    }
}
