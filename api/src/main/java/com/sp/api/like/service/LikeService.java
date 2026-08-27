package com.sp.api.like.service;

import com.sp.api.common.exception.NotFoundException;
import com.sp.api.like.dto.LikeResponse;
import com.sp.api.like.entity.Like;
import com.sp.api.like.repository.LikeRepository;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final StreamRepository streamRepository;

    /**
     * 좋아요를 토글하고 갱신된 상태와 개수를 한 트랜잭션 안에서 함께 돌려준다.
     */
    @Transactional
    public LikeResponse toggle(Long streamId, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new NotFoundException("영상을 찾을 수 없습니다."));

        boolean liked = likeRepository
                .findByStreamIdAndUserId(stream.getId(), user.getId())
                .map(like -> {
                    likeRepository.delete(like);
                    return false;
                })
                .orElseGet(() -> {
                    likeRepository.save(new Like(user, stream));
                    return true;
                });

        // delete/insert 를 반영한 뒤 세어야 정확한 개수가 나온다.
        likeRepository.flush();

        return new LikeResponse(liked, likeRepository.countByStreamId(stream.getId()));
    }

    public long count(Long streamId) {
        return likeRepository.countByStreamId(streamId);
    }

    public boolean isLikedBy(Long streamId, Long userId) {
        return likeRepository.existsByStreamIdAndUserId(streamId, userId);
    }
}
