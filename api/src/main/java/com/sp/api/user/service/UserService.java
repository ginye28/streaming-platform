package com.sp.api.user.service;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ConflictException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.exception.UnauthorizedException;
import com.sp.api.user.dto.ChangePasswordRequest;
import com.sp.api.user.dto.MeResponse;
import com.sp.api.user.dto.UpdateProfileRequest;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("기존 비밀번호와 다른 값을 입력해 주세요.");
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
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
