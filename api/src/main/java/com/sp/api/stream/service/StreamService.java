package com.sp.api.stream.service;

import com.sp.api.stream.dto.CreateStreamRequest;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.dto.UpdateStreamRequest;
import com.sp.api.stream.entity.Stream;
import com.sp.api.stream.repository.StreamRepository;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StreamService {

    private final StreamRepository streamRepository;
    private final UserRepository userRepository;

    public StreamResponse create(CreateStreamRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Stream stream = new Stream();

        stream.setTitle(request.getTitle());
        stream.setDescription(request.getDescription());
        stream.setThumbnailUrl(request.getThumbnailUrl());
        stream.setVideoUrl(request.getVideoUrl());
        stream.setUser(user);

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
        stream.setVideoUrl(request.getVideoUrl());

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
}