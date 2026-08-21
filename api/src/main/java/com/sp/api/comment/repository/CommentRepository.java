package com.sp.api.comment.repository;

import com.sp.api.comment.entity.Comment;
import com.sp.api.common.repository.IdCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // CommentResponse 가 user.nickname 을 읽으므로 함께 조회한다.
    @EntityGraph(attributePaths = "user")
    Page<Comment> findByStreamId(Long streamId, Pageable pageable);

    // 댓글이 실제로 해당 영상에 속하는지까지 확인한다.
    @EntityGraph(attributePaths = "user")
    Optional<Comment> findWithUserByIdAndStreamId(Long id, Long streamId);

    long countByStreamId(Long streamId);

    /** 목록 화면에서 영상별 댓글 수를 한 번에 가져온다. */
    @Query("""
            select new com.sp.api.common.repository.IdCount(c.stream.id, count(c))
            from Comment c
            where c.stream.id in :streamIds
            group by c.stream.id
            """)
    List<IdCount> countByStreamIds(@Param("streamIds") Collection<Long> streamIds);

    /** 영상 삭제 시 FK 제약에 걸리지 않도록 먼저 정리한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.stream.id = :streamId")
    void deleteByStreamId(@Param("streamId") Long streamId);
}
