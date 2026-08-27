package com.sp.api.config;

import com.sp.api.common.jwt.JwtAuthenticationFilter;
import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.common.security.RestAccessDeniedHandler;
import com.sp.api.common.security.RestAuthenticationEntryPoint;
import com.sp.api.user.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // 로그인/회원가입
                        .requestMatchers("/api/auth/**").permitAll()

                        // API 문서
                        .requestMatchers(
                                "/swagger",
                                "/swagger/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 업로드된 정적 파일(썸네일/영상)은 공개
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // nginx-rtmp 가 호출하는 내부 콜백. 반드시 내부망에서만 접근 가능해야 한다.
                        .requestMatchers("/api/internal/**").permitAll()

                        // WebSocket 핸드셰이크. 인증은 STOMP CONNECT 프레임에서 따로 처리한다.
                        .requestMatchers("/ws/**").permitAll()

                        // 방송 설정은 본인만. 아래 공개 규칙보다 먼저 선언한다.
                        .requestMatchers("/api/lives/settings").authenticated()

                        // 라이브 목록·상세·채팅 내역은 비로그인도 볼 수 있어야 한다.
                        .requestMatchers(HttpMethod.GET, "/api/lives/**").permitAll()

                        // 구독 피드는 내가 누구인지 알아야 하므로 공개 규칙보다 먼저 선언한다.
                        .requestMatchers(HttpMethod.GET, "/api/streams/subscribed").authenticated()

                        // 영상 목록/상세/검색/댓글 조회는 비로그인도 볼 수 있어야 한다.
                        .requestMatchers(HttpMethod.GET, "/api/streams/**").permitAll()

                        // 인트로를 봤다는 기록은 비로그인 시청자도 남겨야 한다.
                        // 안 그러면 로그인 안 한 사람에게는 인트로가 매번 다시 뜬다.
                        .requestMatchers(HttpMethod.POST, "/api/channels/*/intro/seen").permitAll()

                        // 채널 페이지 조회도 공개 (구독 토글은 아래 anyRequest 규칙으로 인증 필요)
                        .requestMatchers(HttpMethod.GET, "/api/channels/**").permitAll()

                        // 카테고리 목록 조회는 공개. 생성은 관리자만(아래 /api/categories 규칙).
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()

                        // 신고 처리·카테고리 생성은 관리자만
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categories").hasAuthority("ADMIN")

                        // 업로드는 로그인 사용자만
                        .requestMatchers("/api/files/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
