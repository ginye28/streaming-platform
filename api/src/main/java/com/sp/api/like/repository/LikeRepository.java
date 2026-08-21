package com.sp.api.like.repository;

import com.sp.api.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    // 파라미터는 이름이 아니라 메서드명(StreamId → UserId) 순서로 바인딩된다.
    Optional<Like> findByStreamIdAndUserId(Long streamId, Long userId);

    long countByStreamId(Long streamId);

    boolean existsByStreamIdAndUserId(Long streamId, Long userId);
}
