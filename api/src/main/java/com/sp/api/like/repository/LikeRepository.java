package com.sp.api.like.repository;

import com.sp.api.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByStreamIdAndUserId(Long userId, Long streamId);

    long countByStreamId(Long streamId);

}
