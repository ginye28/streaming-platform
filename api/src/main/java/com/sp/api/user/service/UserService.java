package com.sp.api.user.service;

import com.sp.api.auth.repository.RefreshTokenRepository;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ConflictException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.exception.TooManyRequestsException;
import com.sp.api.common.exception.UnauthorizedException;
import com.sp.api.common.security.SlidingWindowRateLimiter;
import com.sp.api.user.dto.ChangePasswordRequest;
import com.sp.api.user.dto.MeResponse;
import com.sp.api.user.dto.UpdateProfileRequest;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /** 비밀번호 변경 때 현재 비밀번호를 틀린 횟수. 15분에 5번까지. */
    private final SlidingWindowRateLimiter failedPasswordChecks = new SlidingWindowRateLimiter(5, 15 * 60 * 1000L);

    public MeResponse getMe(String email) {
        return MeResponse.from(findByEmail(email));
    }

    @Transactional
    public MeResponse updateProfile(String email, UpdateProfileRequest request) {

        User user = findByEmail(email);

        // 닉네임을 실제로 바꿀 때만 중복 검사한다.
        if (!user.getNickname().equals(request.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }

        user.updateProfile(request.getNickname(), request.getProfileImage());

        return MeResponse.from(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {

        User user = findByEmail(email);

        // 훔친 토큰으로 현재 비밀번호를 대입해 보는 것을 늦춘다. 실패만 센다.
        if (rateLimitEnabled && failedPasswordChecks.isBlocked(email)) {
            throw new TooManyRequestsException(
                    "현재 비밀번호를 여러 번 틀렸습니다. " + failedPasswordChecks.retryAfterSeconds(email) + "초 뒤에 다시 시도해 주세요."
            );
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            if (rateLimitEnabled) {
                failedPasswordChecks.record(email);
            }

            throw new UnauthorizedException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("기존 비밀번호와 다른 값을 입력해 주세요.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));

        failedPasswordChecks.reset(email);

        // 비밀번호를 바꾸는 이유 중 하나는 계정이 남의 손에 넘어간 것 같아서다.
        // 이미 발급된 리프레시 토큰으로는 더 이어 가지 못하게 모두 지운다(액세스 토큰은 만료까지만 남는다).
        refreshTokenRepository.deleteAllByUserId(user.getId());
    }

    public String getStreamKey(String email) {
        return findByEmail(email).getStreamKey();
    }

    @Transactional
    public String regenerateStreamKey(String email) {

        User user = findByEmail(email);
        user.regenerateStreamKey();

        return user.getStreamKey();
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
