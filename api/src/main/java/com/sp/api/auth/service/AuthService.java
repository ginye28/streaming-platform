package com.sp.api.auth.service;

import com.sp.api.auth.dto.LoginRequest;
import com.sp.api.auth.dto.LoginResponse;
import com.sp.api.auth.dto.SignupRequest;
import com.sp.api.common.exception.ConflictException;
import com.sp.api.common.exception.UnauthorizedException;
import com.sp.api.common.jwt.JwtProvider;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // 존재하지 않는 계정과 비밀번호 불일치를 구분하지 않는다.
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return new LoginResponse(jwtProvider.createToken(user.getEmail()));
    }
}
