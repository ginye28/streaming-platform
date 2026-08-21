package com.sp.api.comment.service;

import com.sp.api.comment.dto.CommentResponse;
import com.sp.api.comment.dto.CreateCommentRequest;
import com.sp.api.comment.dto.UpdateCommentRequest;
import com.sp.api.comment.entity.Comment;
import com.sp.api.comment.repository.CommentRepository;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final StreamRepository streamRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse create(Long streamId, CreateCommentRequest request, String email) {

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new NotFoundException("영상을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Comment comment = new Comment(request.getContent(), stream, user);

        return new CommentResponse(commentRepository.save(comment));
    }

    public PageResponse<CommentResponse> findByStream(Long streamId, Pageable pageable) {
        return PageResponse.from(
                commentRepository.findByStreamId(streamId, pageable).map(CommentResponse::new)
        );
    }

    @Transactional
    public CommentResponse update(
            Long streamId,
            Long commentId,
            UpdateCommentRequest request,
            String email
    ) {

        Comment comment = findOwned(streamId, commentId, email, "수정 권한이 없습니다.");

        comment.updateContent(request.getContent());

        return new CommentResponse(comment);
    }

    @Transactional
    public void delete(Long streamId, Long commentId, String email) {
        commentRepository.delete(findOwned(streamId, commentId, email, "삭제 권한이 없습니다."));
    }

    private Comment findOwned(Long streamId, Long commentId, String email, String forbiddenMessage) {

        // streamId 까지 함께 조회해 다른 영상의 댓글을 건드리지 못하게 한다.
        Comment comment = commentRepository.findWithUserByIdAndStreamId(commentId, streamId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));

        if (!comment.isOwnedBy(email)) {
            throw new ForbiddenException(forbiddenMessage);
        }

        return comment;
    }
}
