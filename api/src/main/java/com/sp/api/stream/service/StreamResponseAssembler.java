package com.sp.api.stream.service;

import com.sp.api.comment.repository.CommentRepository;
import com.sp.api.common.repository.IdCount;
import com.sp.api.like.repository.LikeRepository;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.entity.Stream;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 영상 목록에 좋아요·댓글 수와 내 좋아요 여부를 붙인다.
 * 건별로 세면 N+1 이 되므로 id 목록으로 한 번에 집계한다.
 */
@Component
@RequiredArgsConstructor
public class StreamResponseAssembler {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public List<StreamResponse> assemble(List<Stream> streams, String viewerEmail) {

        if (streams.isEmpty()) {
            return List.of();
        }

        List<Long> streamIds = streams.stream().map(Stream::getId).toList();

        Map<Long, Long> likeCounts = IdCount.toMap(likeRepository.countByStreamIds(streamIds));
        Map<Long, Long> commentCounts = IdCount.toMap(commentRepository.countByStreamIds(streamIds));
        Set<Long> likedIds = likedStreamIds(viewerEmail, streamIds);

        return streams.stream()
                .map(stream -> StreamResponse.of(
                        stream,
                        likeCounts.getOrDefault(stream.getId(), 0L),
                        commentCounts.getOrDefault(stream.getId(), 0L),
                        likedIds.contains(stream.getId())
                ))
                .toList();
    }

    public StreamResponse assembleOne(Stream stream, String viewerEmail) {
        return assemble(List.of(stream), viewerEmail).getFirst();
    }

    private Set<Long> likedStreamIds(String viewerEmail, List<Long> streamIds) {

        if (viewerEmail == null) {
            return Set.of();
        }

        return userRepository.findByEmail(viewerEmail)
                .map(user -> Set.copyOf(likeRepository.findLikedStreamIds(user.getId(), streamIds)))
                .orElseGet(Set::of);
    }
}
