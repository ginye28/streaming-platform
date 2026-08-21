package com.sp.api.stream.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
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

        StreamResponse response =
                streamService.create(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StreamResponse>>> findAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        return ResponseEntity.ok(ApiResponse.ok(streamService.findAll(pageable)));
    }

    // 리터럴 경로를 /{id} 보다 먼저 선언해 매핑 의도를 분명히 한다.
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<StreamResponse>>> search(
            @RequestParam @NotBlank String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        return ResponseEntity.ok(ApiResponse.ok(streamService.search(keyword, pageable)));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<StreamResponse>>> popular() {
        return ResponseEntity.ok(ApiResponse.ok(streamService.popular()));
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<StreamResponse>>> latest() {
        return ResponseEntity.ok(ApiResponse.ok(streamService.latest()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(streamService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStreamRequest request,
            Authentication authentication
    ) {

        StreamResponse response = streamService.update(
                id, request, authentication.getName()
        );

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
