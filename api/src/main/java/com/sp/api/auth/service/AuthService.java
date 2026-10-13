package com.sp.api.auth.service;

import com.sp.api.auth.dto.LoginRequest;
import com.sp.api.auth.dto.LoginResponse;
import com.sp.api.auth.dto.RefreshTokenRequest;
import com.sp.api.auth.dto.SignupRequest;
import com.sp.api.auth.entity.RefreshToken;
import com.sp.api.auth.repository.RefreshTokenRepository;
import com.sp.api.common.exception.ConflictException;
import com.sp.api.common.exception.TooManyRequestsException;
import com.sp.api.common.exception.UnauthorizedException;
import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.common.security.SlidingWindowRateLimiter;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /** 계정(이메일)별 로그인 실패. 15분에 10번까지 실패할 수 있다. 서버 하나로 도는 지금 구조에서만 맞다. */
    private final SlidingWindowRateLimiter failedLogins = new SlidingWindowRateLimiter(10, 15 * 60 * 1000L);

    /** 없는 계정의 로그인에서 해시 비교 시간을 맞추려고 쓰는, 아무와도 맞지 않는 해시. */
    private volatile String dummyHash;

    @Transactional
    public void signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname()
        );

        try {
            userRepository.saveAndFlush(user);

        } catch (DataIntegrityViolationException e) {
            // 위 exists 검사와 insert 사이에 동일한 값이 먼저 저장된 경우
            throw new ConflictException("이미 사용 중인 이메일 또는 닉네임입니다.");
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        String attemptKey = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);

        // 한 계정을 여러 IP 에서 돌아가며 노리는 대입 공격을 막는다. 실패만 센다.
        if (rateLimitEnabled && failedLogins.isBlocked(attemptKey)) {
            throw new TooManyRequestsException(
                    "로그인에 여러 번 실패했습니다. " + failedLogins.retryAfterSeconds(attemptKey) + "초 뒤에 다시 시도해 주세요."
            );
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // 없는 계정이어도 비밀번호 해시 비교를 똑같이 한 번 한다. 응답 시간으로 가입 여부를 알아내지 못하게 하려는 것이다.
        String hashToCompare = user == null ? dummyHash() : user.getPassword();
        boolean matches = passwordEncoder.matches(request.getPassword(), hashToCompare);

        // 존재하지 않는 계정과 비밀번호 불일치를 구분하지 않는다.
        if (user == null || !matches) {
            if (rateLimitEnabled) {
                failedLogins.record(attemptKey);
            }

            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        failedLogins.reset(attemptKey);

        return new LoginResponse(jwtProvider.createToken(user.getEmail()), issueRefreshToken(user));
    }

    /** 리프레시 토큰을 소모하고 새 액세스·리프레시 토큰 쌍을 발급한다 (재사용 방지를 위한 회전). */
    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {

        RefreshToken saved = refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken()))
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 리프레시 토큰입니다."));

        refreshTokenRepository.delete(saved);

        if (saved.isExpired()) {
            throw new UnauthorizedException("만료된 리프레시 토큰입니다. 다시 로그인해주세요.");
        }

        User user = saved.getUser();

        return new LoginResponse(jwtProvider.createToken(user.getEmail()), issueRefreshToken(user));
    }

    /** 전달된 리프레시 토큰만 무효화한다. 존재하지 않아도 조용히 끝낸다(멱등). */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.deleteByTokenHash(hash(request.getRefreshToken()));
    }

    private String issueRefreshToken(User user) {

        String rawToken = generateToken();

        refreshTokenRepository.save(new RefreshToken(
                user,
                hash(rawToken),
                Instant.now().plusMillis(refreshExpiration)
        ));

        return rawToken;
    }

    private String dummyHash() {

        String hash = dummyHash;

        if (hash == null) {
            hash = passwordEncoder.encode(generateToken());
            dummyHash = hash;
        }

        return hash;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 모든 JVM 이 지원을 보장하는 표준 알고리즘이라 실제로는 발생하지 않는다.
            throw new IllegalStateException(e);
        }
    }
}
