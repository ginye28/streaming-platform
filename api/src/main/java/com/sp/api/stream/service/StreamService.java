package com.sp.api.stream.service;

import com.sp.api.comment.repository.CommentRepository;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.like.repository.LikeRepository;
import com.sp.api.stream.dto.CreateStreamRequest;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.dto.UpdateStreamRequest;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StreamService {

    private final StreamRepository streamRepository;
    private final UserRepository userRepository;
    private final SubscribeRepository subscribeRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final StreamResponseAssembler assembler;

    @Transactional
    public StreamResponse create(CreateStreamRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Stream stream = new Stream(
                request.getTitle(),
                request.getDescription(),
                request.getThumbnailUrl(),
                request.getVideoUrl(),
                user
        );

        return StreamResponse.ofNew(streamRepository.save(stream));
    }

    public PageResponse<StreamResponse> findAll(Pageable pageable, String viewerEmail) {
        return toPageResponse(streamRepository.findAll(pageable), viewerEmail);
    }

    @Transactional
    public StreamResponse findById(Long id, String viewerEmail) {

        if (streamRepository.increaseViewCount(id) == 0) {
            throw new NotFoundException("영상을 찾을 수 없습니다.");
        }

        Stream stream = streamRepository.findWithUserById(id)
                .orElseThrow(() -> new NotFoundException("영상을 찾을 수 없습니다."));

        return assembler.assembleOne(stream, viewerEmail);
    }

    public PageResponse<StreamResponse> findByChannel(Long channelId, Pageable pageable, String viewerEmail) {
        return toPageResponse(streamRepository.findByUserId(channelId, pageable), viewerEmail);
    }

    /** 구독 중인 채널들의 영상 피드. */
    public PageResponse<StreamResponse> findSubscribedFeed(String email, Pageable pageable) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        List<Long> channelIds = subscribeRepository.findChannelIdsBySubscriberId(user.getId());

        if (channelIds.isEmpty()) {
            return PageResponse.empty(pageable);
        }

        return toPageResponse(streamRepository.findByUserIdIn(channelIds, pageable), email);
    }

    @Transactional
    public StreamResponse update(Long id, UpdateStreamRequest request, String email) {

        Stream stream = findOwned(id, email, "수정 권한이 없습니다.");

        stream.update(
                request.getTitle(),
                request.getDescription(),
                request.getThumbnailUrl(),
                request.getVideoUrl()
        );

        return assembler.assembleOne(stream, email);
    }

    @Transactional
    public void delete(Long id, String email) {

        Stream stream = findOwned(id, email, "삭제 권한이 없습니다.");

        // 댓글·좋아요가 FK 로 영상을 참조하므로 먼저 정리해야 한다.
        commentRepository.deleteByStreamId(id);
        likeRepository.deleteByStreamId(id);

        streamRepository.delete(stream);
    }

    public PageResponse<StreamResponse> search(String keyword, Pageable pageable, String viewerEmail) {
        return toPageResponse(streamRepository.search(keyword, pageable), viewerEmail);
    }

    public List<StreamResponse> popular(String viewerEmail) {
        return assembler.assemble(streamRepository.findTop10ByOrderByViewCountDesc(), viewerEmail);
    }

    public List<StreamResponse> latest(String viewerEmail) {
        return assembler.assemble(streamRepository.findTop10ByOrderByCreatedAtDesc(), viewerEmail);
    }

    private PageResponse<StreamResponse> toPageResponse(Page<Stream> page, String viewerEmail) {
        return PageResponse.of(page, assembler.assemble(page.getContent(), viewerEmail));
    }

    private Stream findOwned(Long id, String email, String forbiddenMessage) {

        Stream stream = streamRepository.findWithUserById(id)
                .orElseThrow(() -> new NotFoundException("영상을 찾을 수 없습니다."));

        if (!stream.isOwnedBy(email)) {
            throw new ForbiddenException(forbiddenMessage);
        }

        return stream;
    }
}
