package com.sp.api.stream.service;

import com.sp.api.stream.dto.CreateStreamRequest;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.dto.UpdateStreamRequest;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.entity.StreamStatus;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StreamService {

    private final StreamRepository streamRepository;
    private final UserRepository userRepository;

    @Value("${streaming.hls-url}")
    private String hlsUrl;

    public StreamResponse create(CreateStreamRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Stream stream = new Stream();

        stream.setTitle(request.getTitle());
        stream.setDescription(request.getDescription());
        stream.setThumbnailUrl(request.getThumbnailUrl());
        stream.setUser(user);
        stream.setStreamKey(UUID.randomUUID().toString());
        stream.setVideoUrl(hlsUrl + "/" + stream.getStreamKey() + ".m3u8");

        Stream saved = streamRepository.save(stream);

        return new StreamResponse(saved);
    }

    public List<StreamResponse> findAll() {
        return streamRepository.findAll()
                .stream()
                .map(StreamResponse::new)
                .toList();
    }

    public StreamResponse findById(Long id) {

        Stream stream = streamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        stream.increaseViewCount();
        streamRepository.save(stream);

        return new StreamResponse(stream);
    }

    public StreamResponse update(
            Long id,
            UpdateStreamRequest request,
            String email
    ) {

        Stream stream = streamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        if (!stream.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        stream.setTitle(request.getTitle());
        stream.setDescription(request.getDescription());
        stream.setThumbnailUrl(request.getThumbnailUrl());

        Stream updated = streamRepository.save(stream);

        return new StreamResponse(updated);
    }

    public void delete(Long id, String email) {

        Stream stream = streamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

        if (!stream.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        streamRepository.delete(stream);
    }

    public List<StreamResponse> search(String keyword) {

        return streamRepository.findByTitleContainingIgnoreCase(keyword)
                .stream()
                .map(StreamResponse::new)
                .toList();
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

    public StreamResponse start(Long id, String email) {

        Stream stream = streamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));

        if (!stream.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("방송 시작 권한이 없습니다.");
        }

        if (stream.getStatus() != StreamStatus.READY) {
            throw new IllegalArgumentException("방송을 시작할 수 없는 상태입니다.");
        }

        stream.setStatus(StreamStatus.LIVE);

        Stream saved = streamRepository.save(stream);

        return new StreamResponse(saved);
    }

    public StreamResponse stop(Long id, String email) {

        Stream stream = streamRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다."));

        if (!stream.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("방송 종료 권한이 없습니다.");
        }

        if (stream.getStatus() != StreamStatus.LIVE) {
            throw new IllegalArgumentException("현재 방송 중이 아닙니다.");
        }

        stream.setStatus(StreamStatus.ENDED);

        Stream saved = streamRepository.save(stream);

        return new StreamResponse(saved);
    }
}