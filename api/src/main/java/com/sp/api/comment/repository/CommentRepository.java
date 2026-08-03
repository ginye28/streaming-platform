package com.sp.api.comment.repository;

import com.sp.api.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByStreamId(Long streamId);
}