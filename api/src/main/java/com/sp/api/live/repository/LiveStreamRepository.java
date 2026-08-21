package com.sp.api.live.repository;

import com.sp.api.live.entity.LiveStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Long> {

    @EntityGraph(attributePaths = "user")
    Page<LiveStream> findByStatusOrderByStartedAtDesc(LiveStream.Status status, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Optional<LiveStream> findWithUserById(Long id);

    /** 한 사용자는 동시에 하나의 방송만 LIVE 일 수 있다. */
    @EntityGraph(attributePaths = "user")
    Optional<LiveStream> findByUserIdAndStatus(Long userId, LiveStream.Status status);

    boolean existsByUserIdAndStatus(Long userId, LiveStream.Status status);

    /** 채널 목록에서 어느 채널이 방송 중인지 한 번에 판별한다. */
    @org.springframework.data.jpa.repository.Query(
            "select l.user.id from LiveStream l where l.status = :status and l.user.id in :userIds")
    List<Long> findLiveUserIds(
            @org.springframework.data.repository.query.Param("status") LiveStream.Status status,
            @org.springframework.data.repository.query.Param("userIds") Collection<Long> userIds);

    /** 지난 방송 다시보기 목록. */
    @EntityGraph(attributePaths = "user")
    Page<LiveStream> findByUserIdAndStatusOrderByStartedAtDesc(
            Long userId, LiveStream.Status status, Pageable pageable);
}
