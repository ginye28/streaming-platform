package com.sp.api.comment.service;

import com.sp.api.comment.dto.CommentResponse;
import com.sp.api.comment.dto.CreateCommentRequest;
import com.sp.api.comment.dto.UpdateCommentRequest;
import com.sp.api.comment.entity.Comment;
import com.sp.api.comment.repository.CommentRepository;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final StreamRepository streamRepository;
    private final UserRepository userRepository;

    public CommentResponse create(
            Long streamId,
            CreateCommentRequest request,
            String email
    ) {

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Comment comment = new Comment();

        comment.setContent(request.getContent());
        comment.setStream(stream);
        comment.setUser(user);

        Comment saved = commentRepository.save(comment);

        return new CommentResponse(saved);
    }

    public List<CommentResponse> findByStream(Long streamId) {

        return commentRepository.findByStreamId(streamId)
                .stream()
                .map(CommentResponse::new)
                .toList();
    }

    public CommentResponse update(
            Long commentId,
            UpdateCommentRequest request,
            String email
    ) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        comment.setContent(request.getContent());

        Comment updated = commentRepository.save(comment);

        return new CommentResponse(updated);
    }

    public void delete(
            Long commentId,
            String email
    ) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }
}