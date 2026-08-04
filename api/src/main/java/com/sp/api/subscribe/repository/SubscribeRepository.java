package com.sp.api.subscribe.repository;

import com.sp.api.subscribe.entity.Subscribe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscribeRepository extends JpaRepository<Subscribe, Long> {

    Optional<Subscribe> findBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    long countByChannelId(Long channelId);
}
