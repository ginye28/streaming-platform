package com.sp.api.subscribe.repository;

import com.sp.api.subscribe.entity.Subscribe;
import com.sp.api.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    /**
     * 주어진 사람들 중 이 채널을 구독한 사람의 등급과 마크 표시 여부.
     * 채팅·댓글 한 페이지의 오시마크를 한 번에 가린다.
     */
    @Query("""
            select new com.sp.api.subscribe.repository.SubscriberMark(
                s.subscriber.id, s.tier, s.markVisible, s.paidUntil)
            from Subscribe s
            where s.channel.id = :channelId and s.subscriber.id in :userIds
            """)
    List<SubscriberMark> findSubscriberMarksAmong(
            @Param("channelId") Long channelId, @Param("userIds") Collection<Long> userIds);

    /** 이 시청자가 주어진 채널들에서 가진 구독 등급. 구독하지 않은 채널은 나오지 않는다. */
    @Query("""
            select new com.sp.api.subscribe.repository.ChannelTier(s.channel.id, s.tier, s.paidUntil)
            from Subscribe s
            where s.subscriber.id = :subscriberId and s.channel.id in :channelIds
            """)
    List<ChannelTier> findTiersOfSubscriber(
            @Param("subscriberId") Long subscriberId, @Param("channelIds") Collection<Long> channelIds);

    /**
     * 유료 기간이 now ~ until 사이에 끝나고, 그 만료에 대해 아직 알리지 않은 구독.
     * 연장하면 paidUntil 이 바뀌어 expiryNotifiedFor 와 달라지므로 다음 만료 때 다시 잡힌다.
     */
    @EntityGraph(attributePaths = {"subscriber", "channel"})
    @Query("""
            select s from Subscribe s
            where s.tier = com.sp.api.subscribe.entity.SubscriptionTier.PAID
              and s.paidUntil > :now and s.paidUntil <= :until
              and (s.expiryNotifiedFor is null or s.expiryNotifiedFor <> s.paidUntil)
            """)
    List<Subscribe> findExpiringPaid(
            @Param("now") java.time.LocalDateTime now, @Param("until") java.time.LocalDateTime until);

    /** 구독 피드용 채널 id 목록. */
    @Query("select s.channel.id from Subscribe s where s.subscriber.id = :subscriberId")
    List<Long> findChannelIdsBySubscriberId(@Param("subscriberId") Long subscriberId);
}
