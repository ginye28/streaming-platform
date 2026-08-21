package com.sp.api.comment.repository;

import com.sp.api.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // CommentResponse 가 user.nickname 을 읽으므로 함께 조회한다.
    @EntityGraph(attributePaths = "user")
    Page<Comment> findByStreamId(Long streamId, Pageable pageable);

    // 댓글이 실제로 해당 영상에 속하는지까지 확인한다.
    @EntityGraph(attributePaths = "user")
    Optional<Comment> findWithUserByIdAndStreamId(Long id, Long streamId);
}
