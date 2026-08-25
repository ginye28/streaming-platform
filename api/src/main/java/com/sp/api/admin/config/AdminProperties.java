package com.sp.api.admin.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Component
public class AdminProperties {

    /**
     * 기동 시 ADMIN 권한을 부여할 계정 이메일 목록.
     *
     * 최초 관리자를 만들기 위한 통로다. 관리자가 한 명도 없으면 승격 API 도 쓸 수 없어
     * (그 API 자체가 ADMIN 을 요구한다) 설정으로 시작점을 열어 준다.
     * 여기 적힌 계정은 먼저 일반 회원가입이 되어 있어야 한다.
     */
    @Value("${app.admin.emails:}")
    private List<String> emails;
}
