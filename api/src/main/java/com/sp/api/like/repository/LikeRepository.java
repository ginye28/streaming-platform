package com.sp.api.like.repository;

import com.sp.api.common.repository.IdCount;
import com.sp.api.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    // 파라미터는 이름이 아니라 메서드명(StreamId → UserId) 순서로 바인딩된다.
    Optional<Like> findByStreamIdAndUserId(Long streamId, Long userId);

    long countByStreamId(Long streamId);

    boolean existsByStreamIdAndUserId(Long streamId, Long userId);

    /** 목록 화면에서 영상별 좋아요 수를 한 번에 가져온다. */
    @Query("""
            select new com.sp.api.common.repository.IdCount(l.stream.id, count(l))
            from Like l
            where l.stream.id in :streamIds
            group by l.stream.id
            """)
    List<IdCount> countByStreamIds(@Param("streamIds") Collection<Long> streamIds);

    /** 목록 중 내가 좋아요를 누른 영상 id 들. */
    @Query("select l.stream.id from Like l where l.user.id = :userId and l.stream.id in :streamIds")
    List<Long> findLikedStreamIds(@Param("userId") Long userId,
                                  @Param("streamIds") Collection<Long> streamIds);

    /** 영상 삭제 시 FK 제약에 걸리지 않도록 먼저 정리한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Like l where l.stream.id = :streamId")
    void deleteByStreamId(@Param("streamId") Long streamId);
}
