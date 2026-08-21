package com.sp.api.live.service;

import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LiveStreamService {

    private final UserRepository userRepository;

    /**
     * nginx-rtmp 가 송출 시작 시 넘겨준 스트림 키를 검증한다.
     * 이 검증이 없으면 누구나 임의의 이름으로 RTMP 송출을 할 수 있다.
     */
    public User authorizePublish(String streamKey) {

        if (streamKey == null || streamKey.isBlank()) {
            throw new ForbiddenException("스트림 키가 필요합니다.");
        }

        return userRepository.findByStreamKey(streamKey)
                .orElseThrow(() -> {
                    log.warn("알 수 없는 스트림 키로 송출 시도");
                    return new ForbiddenException("유효하지 않은 스트림 키입니다.");
                });
    }
}
