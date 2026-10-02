package com.sp.api.live.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiveScheduleRepository extends JpaRepository<LiveSchedule, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<LiveSchedule> findWithUserById(Long id);

    long countByUserIdAndStatus(Long userId, LiveSchedule.Status status);

    /**
     * 앞으로의 방송 예약. 방송 시각이 since 보다 늦은 것만(방송이 조금 늦어지는 경우를 위해 since 는 지금보다 앞이다).
     * excludedUserIds 는 비어 있으면 안 된다 (JPQL 의 not in 은 빈 컬렉션을 못 받는다).
     */
    @EntityGraph(attributePaths = "user")
    @Query("""
            select s from LiveSchedule s
            where s.status = com.sp.api.live.schedule.LiveSchedule.Status.SCHEDULED
              and s.scheduledAt > :since and s.user.id not in :excludedUserIds
            order by s.scheduledAt asc
            """)
    Page<LiveSchedule> findUpcoming(
            @Param("since") LocalDateTime since,
            @Param("excludedUserIds") Collection<Long> excludedUserIds,
            Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select s from LiveSchedule s
            where s.user.id = :userId
              and s.status = com.sp.api.live.schedule.LiveSchedule.Status.SCHEDULED
              and s.scheduledAt > :since
            order by s.scheduledAt asc
            """)
    List<LiveSchedule> findUpcomingOfChannel(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** 내 예약. 아직 방송 전인 것을 가까운 순으로, 시각이 지난 것도 상태가 SCHEDULED 면 보인다. */
    @EntityGraph(attributePaths = "user")
    List<LiveSchedule> findByUserIdAndStatusOrderByScheduledAtAsc(Long userId, LiveSchedule.Status status);

    /** 방송이 시작될 때 이어 붙일 예약 후보. 시각이 가까운 순. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            select s from LiveSchedule s
            where s.user.id = :userId
              and s.status = com.sp.api.live.schedule.LiveSchedule.Status.SCHEDULED
              and s.scheduledAt between :from and :to
            order by s.scheduledAt asc
            """)
    List<LiveSchedule> findClaimable(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    /** "곧 시작" 알림을 보낼 때가 된 예약. */
    @EntityGraph(attributePaths = "user")
    @Query("""
            select s from LiveSchedule s
            where s.status = com.sp.api.live.schedule.LiveSchedule.Status.SCHEDULED
              and s.reminderSentAt is null
              and s.scheduledAt between :from and :to
            """)
    List<LiveSchedule> findDueForReminder(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** 시각이 한참 지난 채 방송하지 않은 예약. */
    @Query("""
            select s from LiveSchedule s
            where s.status = com.sp.api.live.schedule.LiveSchedule.Status.SCHEDULED
              and s.scheduledAt < :before
            """)
    List<LiveSchedule> findStale(@Param("before") LocalDateTime before);
}
