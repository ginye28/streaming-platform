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

    // 목록에는 원 댓글만 싣는다. 답글은 아래에서 묶어 온다.
    // CommentResponse 가 user.nickname 을 읽으므로 함께 조회한다.
    @EntityGraph(attributePaths = "user")
    Page<Comment> findByStreamIdAndParentIsNull(Long streamId, Pageable pageable);

    /**
     * 이 페이지에 실린 원 댓글들의 답글을 한 번에 가져온다.
     * 이름으로 만드는 쿼리(findByParentIdIn...)를 쓰면 Comment.getParentId() 때문에
     * parentId 를 컬럼으로 착각하니 JPQL 을 직접 적는다.
     */
    @EntityGraph(attributePaths = {"user", "parent"})
    @Query("select c from Comment c where c.parent.id in :parentIds order by c.createdAt asc")
    List<Comment> findRepliesOf(@Param("parentIds") Collection<Long> parentIds);

    // 댓글이 실제로 해당 영상에 속하는지까지 확인한다.
    @EntityGraph(attributePaths = {"user", "parent"})
    Optional<Comment> findWithUserByIdAndStreamId(Long id, Long streamId);

    long countByStreamId(Long streamId);

    /** 목록 화면에서 영상별 댓글 수를 한 번에 가져온다. 답글도 함께 센다. */
    @Query("""
            select new com.sp.api.common.repository.IdCount(c.stream.id, count(c))
            from Comment c
            where c.stream.id in :streamIds
            group by c.stream.id
            """)
    List<IdCount> countByStreamIds(@Param("streamIds") Collection<Long> streamIds);

    /** 원 댓글을 지우기 전에 거기 달린 답글부터 정리한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.parent.id = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);

    /** 영상 삭제 시 FK 제약에 걸리지 않도록, 원 댓글보다 답글을 먼저 지운다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.stream.id = :streamId and c.parent is not null")
    void deleteRepliesByStreamId(@Param("streamId") Long streamId);

    /** 영상 삭제 시 FK 제약에 걸리지 않도록 먼저 정리한다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Comment c where c.stream.id = :streamId")
    void deleteByStreamId(@Param("streamId") Long streamId);
}
