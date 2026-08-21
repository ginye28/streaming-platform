package com.sp.api.subscribe.repository;

import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.user.entity.User;
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

    /** 방송 시작 알림을 보낼 구독자들. */
    @Query("select s.subscriber from Subscribe s where s.channel.id = :channelId")
    List<User> findSubscribersOfChannel(@Param("channelId") Long channelId);

    /** 구독 피드용 채널 id 목록. */
    @Query("select s.channel.id from Subscribe s where s.subscriber.id = :subscriberId")
    List<Long> findChannelIdsBySubscriberId(@Param("subscriberId") Long subscriberId);
}
