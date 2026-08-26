package com.sp.api.stream.service;

import com.sp.api.block.service.BlockService;
import com.sp.api.category.entity.Category;
import com.sp.api.category.repository.CategoryRepository;
import com.sp.api.comment.repository.CommentRepository;
import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.like.repository.LikeRepository;
import com.sp.api.stream.dto.CreateStreamRequest;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.dto.StreamSort;
import com.sp.api.stream.dto.UpdateStreamRequest;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.subscribe.repository.SubscribeRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final CategoryRepository categoryRepository;
    private final BlockService blockService;
    private final StreamResponseAssembler assembler;
    private final StreamViewGuard viewGuard;

    /** 인기·최신 목록에서 보여 줄 개수. */
    private static final int TOP_SIZE = 10;

    @Transactional
    public StreamResponse create(CreateStreamRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        Stream stream = new Stream(
                request.getTitle(),
                request.getDescription(),
                request.getThumbnailUrl(),
                request.getVideoUrl(),
                user,
                findCategoryOrNull(request.getCategoryId())
        );

        return StreamResponse.ofNew(streamRepository.save(stream));
    }

    /** 전체 목록. categoryId 로 좁힐 수 있고, 로그인 상태면 내가 차단한 채널의 영상은 제외한다. */
    public PageResponse<StreamResponse> findAll(
            Pageable pageable, StreamSort sort, String viewerEmail, Long categoryId) {

        return toPageResponse(
                streamRepository.findAllFiltered(
                        categoryId, blockService.excludedUserIds(viewerEmail), withSort(pageable, sort)),
                viewerEmail
        );
    }

    /**
     * 영상 하나. 조회수는 같은 시청자에 대해 일정 시간에 한 번만 올린다.
     * viewerKey 는 로그인했으면 계정, 아니면 접속 IP 다.
     */
    @Transactional
    public StreamResponse findById(Long id, String viewerEmail, String viewerKey) {

        if (viewGuard.shouldCount(id, viewerKey) && streamRepository.increaseViewCount(id) == 0) {
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
                request.getVideoUrl(),
                findCategoryOrNull(request.getCategoryId())
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
        return top(StreamSort.POPULAR, viewerEmail);
    }

    public List<StreamResponse> latest(String viewerEmail) {
        return top(StreamSort.LATEST, viewerEmail);
    }

    /**
     * 상위 몇 개만 보여 주는 목록.
     * 전체 목록과 같은 쿼리를 써서, 차단한 사용자의 영상이 이쪽으로 새지 않게 한다.
     */
    private List<StreamResponse> top(StreamSort sort, String viewerEmail) {

        Page<Stream> page = streamRepository.findAllFiltered(
                null,
                blockService.excludedUserIds(viewerEmail),
                PageRequest.of(0, TOP_SIZE, sort.toSort()));

        return assembler.assemble(page.getContent(), viewerEmail);
    }

    /** 정렬은 sortBy 로만 정한다. 요청에 실려 온 Pageable 의 정렬은 쓰지 않는다. */
    private Pageable withSort(Pageable pageable, StreamSort sort) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.toSort());
    }

    private PageResponse<StreamResponse> toPageResponse(Page<Stream> page, String viewerEmail) {
        return PageResponse.of(page, assembler.assemble(page.getContent(), viewerEmail));
    }

    private Category findCategoryOrNull(Long categoryId) {

        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("카테고리를 찾을 수 없습니다."));
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
