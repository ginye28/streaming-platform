package com.sp.api.stream.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.common.security.AuthUtils;
import com.sp.api.stream.dto.CreateStreamRequest;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.dto.UpdateStreamRequest;
import com.sp.api.stream.service.StreamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/streams")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    @PostMapping
    public ResponseEntity<ApiResponse<StreamResponse>> create(
            @Valid @RequestBody CreateStreamRequest request,
            Authentication authentication
    ) {

        StreamResponse response = streamService.create(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StreamResponse>>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                streamService.findAll(pageable, AuthUtils.emailOrNull(authentication))
        ));
    }

    // 리터럴 경로들을 /{id} 보다 먼저 선언한다.

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<StreamResponse>>> search(
            @RequestParam @NotBlank String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                streamService.search(keyword, pageable, AuthUtils.emailOrNull(authentication))
        ));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<StreamResponse>>> popular(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                streamService.popular(AuthUtils.emailOrNull(authentication))
        ));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<StreamResponse>>> latest(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(
                streamService.latest(AuthUtils.emailOrNull(authentication))
        ));
    }

    /** 구독 중인 채널들의 영상 피드. 로그인 필요. */
    @GetMapping("/subscribed")
    public ResponseEntity<ApiResponse<PageResponse<StreamResponse>>> subscribedFeed(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                streamService.findSubscribedFeed(authentication.getName(), pageable)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamResponse>> findById(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                streamService.findById(id, AuthUtils.emailOrNull(authentication))
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStreamRequest request,
            Authentication authentication
    ) {

        StreamResponse response = streamService.update(id, request, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {

        streamService.delete(id, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
