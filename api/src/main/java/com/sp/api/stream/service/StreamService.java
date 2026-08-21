package com.sp.api.stream.service;

import com.sp.api.common.exception.ForbiddenException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.stream.dto.CreateStreamRequest;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.dto.UpdateStreamRequest;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

        return new StreamResponse(streamRepository.save(stream));
    }

    public PageResponse<StreamResponse> findAll(Pageable pageable) {
        return PageResponse.from(
                streamRepository.findAll(pageable).map(StreamResponse::new)
        );
    }

    @Transactional
    public StreamResponse findById(Long id) {

        if (streamRepository.increaseViewCount(id) == 0) {
            throw new NotFoundException("영상을 찾을 수 없습니다.");
        }

        Stream stream = streamRepository.findWithUserById(id)
                .orElseThrow(() -> new NotFoundException("영상을 찾을 수 없습니다."));

        return new StreamResponse(stream);
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

        return new StreamResponse(stream);
    }

    @Transactional
    public void delete(Long id, String email) {
        streamRepository.delete(findOwned(id, email, "삭제 권한이 없습니다."));
    }

    public PageResponse<StreamResponse> search(String keyword, Pageable pageable) {
        return PageResponse.from(
                streamRepository.findByTitleContainingIgnoreCase(keyword, pageable)
                        .map(StreamResponse::new)
        );
    }

    public List<StreamResponse> popular() {
        return streamRepository.findTop10ByOrderByViewCountDesc()
                .stream()
                .map(StreamResponse::new)
                .toList();
    }

    public List<StreamResponse> latest() {
        return streamRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(StreamResponse::new)
                .toList();
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
