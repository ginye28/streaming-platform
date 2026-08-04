package com.sp.api.like.service;

import com.sp.api.like.entity.Like;
import com.sp.api.like.repository.LikeRepository;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final StreamRepository streamRepository;

    public boolean toggle(Long streamId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        return likeRepository.findByStreamIdAndUserId(user.getId(), stream.getId())
                .map(like -> {
                    likeRepository.delete(like);
                    return false;
                })
                .orElseGet(() -> {
                    Like like = new Like();
                    like.setUser(user);
                    like.setStream(stream);
                    likeRepository.save(like);
                    return true;
                });
    }

    public long count(Long streamId) {
        return likeRepository.countByStreamId(streamId);
    }
}