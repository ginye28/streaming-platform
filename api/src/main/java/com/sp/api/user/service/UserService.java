package com.sp.api.user.service;

import com.sp.api.common.exception.NotFoundException;
import com.sp.api.user.dto.MeResponse;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public MeResponse getMe(String email) {
        return MeResponse.from(findByEmail(email));
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
