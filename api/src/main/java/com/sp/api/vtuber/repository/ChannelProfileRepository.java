package com.sp.api.vtuber.repository;

import com.sp.api.vtuber.entity.ChannelProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelProfileRepository extends JpaRepository<ChannelProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<ChannelProfile> findByUserId(Long userId);
}
