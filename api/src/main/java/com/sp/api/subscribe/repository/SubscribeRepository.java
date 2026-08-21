package com.sp.api.subscribe.repository;

import com.sp.api.subscribe.entity.Subscribe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscribeRepository extends JpaRepository<Subscribe, Long> {

    Optional<Subscribe> findBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    boolean existsBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    long countByChannelId(Long channelId);

    /** 내가 구독 중인 채널 목록. */
    @EntityGraph(attributePaths = "channel")
    Page<Subscribe> findBySubscriberId(Long subscriberId, Pageable pageable);

    /** 구독 피드용 채널 id 목록. */
    @Query("select s.channel.id from Subscribe s where s.subscriber.id = :subscriberId")
    List<Long> findChannelIdsBySubscriberId(@Param("subscriberId") Long subscriberId);
}
