package com.sp.api.comment.service;

import com.sp.api.comment.dto.CommentResponse;
import com.sp.api.comment.dto.CreateCommentRequest;
import com.sp.api.comment.dto.UpdateCommentRequest;
import com.sp.api.comment.entity.Comment;
import com.sp.api.comment.repository.CommentRepository;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.notification.service.NotificationService;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import com.sp.api.vtuber.dto.OshiMark;
import com.sp.api.vtuber.service.ChannelProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final StreamRepository streamRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ChannelProfileService channelProfileService;

    @Transactional
    public CommentResponse create(Long streamId, CreateCommentRequest request, String email) {

        Stream stream = streamRepository.findById(streamId)
                .orElseThrow(() -> new NotFoundException("영상을 찾을 수 없습니다."));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Comment parent = findParentOrNull(streamId, request.getParentId());

        Comment saved = commentRepository.save(
                new Comment(request.getContent(), stream, user, parent)
        );

        OshiMark mark = markOf(stream.getUser().getId(), user.getId());

        if (parent == null) {
            notificationService.notifyComment(saved);
            return CommentResponse.withReplies(saved, List.of(), mark);
        }

        notificationService.notifyReply(saved);

        return CommentResponse.reply(saved, parent.getId(), mark);
    }

    /**
     * 원 댓글만 페이지로 나누고, 그 페이지에 실린 댓글들의 답글은 통째로 딸려 보낸다.
     * 답글까지 페이지를 나누면 화면과 계산이 같이 복잡해지는데, 한 댓글에 달리는 답글은 대개 몇 개뿐이다.
     */
    public PageResponse<CommentResponse> findByStream(Long streamId, Pageable pageable) {

        Page<Comment> roots = commentRepository.findByStreamIdAndParentIsNullOrderByCreatedAtDescIdDesc(streamId, pageable);

        // 오시마크는 이 영상의 채널 기준이다. 페이지 전체 작성자를 모아 한 번에 가린다.
        Long channelId = streamRepository.findById(streamId)
                .map(stream -> stream.getUser().getId())
                .orElse(null);

        List<Comment> replies = roots.getContent().isEmpty()
                ? List.of()
                : commentRepository.findRepliesOf(
                        roots.getContent().stream().map(Comment::getId).toList());

        Map<Long, OshiMark> marks = marksOfAuthors(channelId, roots.getContent(), replies);

        Map<Long, List<CommentResponse>> repliesByParent = replies.stream()
                .collect(Collectors.groupingBy(
                        Comment::getParentId,
                        Collectors.mapping(
                                reply -> CommentResponse.reply(
                                        reply, reply.getParentId(), marks.get(reply.getUser().getId())),
                                Collectors.toList())
                ));

        List<CommentResponse> content = roots.getContent().stream()
                .map(root -> CommentResponse.withReplies(
                        root,
                        repliesByParent.getOrDefault(root.getId(), List.of()),
                        marks.get(root.getUser().getId())))
                .toList();

        return PageResponse.of(roots, content);
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

        return CommentResponse.of(
                comment,
                markOf(comment.getStream().getUser().getId(), comment.getUser().getId()));
    }

    @Transactional
    public void delete(Long streamId, Long commentId, String email) {

        findOwned(streamId, commentId, email, "삭제 권한이 없습니다.");

        // 답글이 원 댓글을 FK 로 참조하므로 답글부터 지운다.
        commentRepository.deleteByParentId(commentId);
        commentRepository.deleteById(commentId);
    }

    /** 이 페이지 댓글·답글 작성자들 가운데 오시마크를 달 사람. 쿼리 한 번으로 가린다. */
    private Map<Long, OshiMark> marksOfAuthors(
            Long channelId, List<Comment> roots, List<Comment> replies
    ) {

        if (channelId == null) {
            return Map.of();
        }

        Set<Long> authorIds = java.util.stream.Stream.concat(roots.stream(), replies.stream())
                .map(comment -> comment.getUser().getId())
                .collect(Collectors.toSet());

        return channelProfileService.oshiMarksFor(channelId, authorIds);
    }

    /** 방금 쓴 댓글 한 건의 오시마크. */
    private OshiMark markOf(Long channelId, Long authorId) {
        return channelProfileService.oshiMarksFor(channelId, Set.of(authorId)).get(authorId);
    }

    /** 답글의 답글은 만들지 않는다. 화면에서도 답글에는 답글 버튼을 두지 않는다. */
    private Comment findParentOrNull(Long streamId, Long parentId) {

        if (parentId == null) {
            return null;
        }

        // 다른 영상의 댓글에 답글이 붙지 않도록 streamId 까지 함께 확인한다.
        Comment parent = commentRepository.findWithUserByIdAndStreamId(parentId, streamId)
                .orElseThrow(() -> new NotFoundException("원 댓글을 찾을 수 없습니다."));

        if (parent.isReply()) {
            throw new BadRequestException("답글에는 다시 답글을 달 수 없습니다.");
        }

        return parent;
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
