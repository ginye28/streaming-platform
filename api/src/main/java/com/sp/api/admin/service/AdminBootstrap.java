package com.sp.api.admin.service;

import com.sp.api.admin.config.AdminProperties;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * app.admin.emails 에 적힌 계정을 기동 시 ADMIN 으로 올린다.
 * 관리자가 한 명도 없으면 승격 API 를 쓸 수 없으므로(그 API 가 ADMIN 을 요구한다)
 * 최초 관리자는 이 경로로만 만들 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AdminProperties adminProperties;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        adminProperties.getEmails().stream()
                .map(String::trim)
                .filter(email -> !email.isBlank())
                .forEach(this::promote);
    }

    private void promote(String email) {

        userRepository.findByEmail(email).ifPresentOrElse(
                user -> {
                    if (user.isAdmin()) {
                        return;
                    }

                    user.changeRole(User.Role.ADMIN);
                    log.info("관리자로 승격: {}", email);
                },
                () -> log.warn("app.admin.emails 의 계정을 찾을 수 없습니다. 먼저 회원가입이 필요합니다: {}", email)
        );
    }
}
