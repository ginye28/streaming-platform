package com.sp.api.intro.repository;

import com.sp.api.intro.entity.ChannelIntro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelIntroRepository extends JpaRepository<ChannelIntro, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<ChannelIntro> findByUserId(Long userId);
}
