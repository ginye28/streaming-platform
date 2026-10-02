package com.sp.api.chat.moderation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChannelModeratorRepository extends JpaRepository<ChannelModerator, Long> {

    boolean existsByChannelIdAndUserId(Long channelId, Long userId);

    Optional<ChannelModerator> findByChannelIdAndUserId(Long channelId, Long userId);

    long countByChannelId(Long channelId);

    @EntityGraph(attributePaths = "user")
    List<ChannelModerator> findByChannelIdOrderByIdDesc(Long channelId);

    /** 주어진 사람들 중 이 채널의 매니저인 사람. 채팅 한 페이지의 배지를 한 번에 가린다. */
    @Query("select m.user.id from ChannelModerator m where m.channel.id = :channelId and m.user.id in :userIds")
    List<Long> findModeratorIdsAmong(
            @Param("channelId") Long channelId, @Param("userIds") Collection<Long> userIds);
}
