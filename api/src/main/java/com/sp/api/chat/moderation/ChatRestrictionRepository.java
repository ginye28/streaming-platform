package com.sp.api.chat.moderation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRestrictionRepository extends JpaRepository<ChatRestriction, Long> {

    Optional<ChatRestriction> findByChannelIdAndUserId(Long channelId, Long userId);

    /** 지금도 막혀 있는 사람들. 시간이 지난 일시 정지는 뺀다. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            select r from ChatRestriction r
            where r.channel.id = :channelId
              and (r.restrictedUntil is null or r.restrictedUntil > :now)
            order by r.id desc
            """)
    List<ChatRestriction> findActive(@Param("channelId") Long channelId, @Param("now") LocalDateTime now);
}
