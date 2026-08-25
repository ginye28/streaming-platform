package com.sp.api.integration;

import com.sp.api.admin.service.AdminBootstrap;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 최초 관리자 부트스트랩. 기동 시점에는 계정이 아직 없을 수 있으므로
 * 계정을 만든 뒤 러너를 직접 실행해 승격 로직만 확인한다.
 */
@SpringBootTest(properties = "app.admin.emails=boot-admin@test.com,없는계정@test.com")
@Transactional
class AdminBootstrapTest {

    @Autowired
    private AdminBootstrap adminBootstrap;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("설정에 적힌 계정은 ADMIN 으로 승격되고, 없는 계정은 건너뛴다")
    void promotesConfiguredEmails() {

        userRepository.saveAndFlush(
                new User("boot-admin@test.com", passwordEncoder.encode("password123"), "부트관리자"));
        userRepository.saveAndFlush(
                new User("other@test.com", passwordEncoder.encode("password123"), "무관한사람"));

        adminBootstrap.run(null);

        assertThat(userRepository.findByEmail("boot-admin@test.com").orElseThrow().getRole())
                .isEqualTo(User.Role.ADMIN);

        // 설정에 없는 계정은 그대로 USER
        assertThat(userRepository.findByEmail("other@test.com").orElseThrow().getRole())
                .isEqualTo(User.Role.USER);
    }
}
