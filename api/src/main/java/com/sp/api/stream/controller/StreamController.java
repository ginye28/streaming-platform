package com.sp.api.stream.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.stream.dto.CreateStreamRequest;
import com.sp.api.stream.dto.StreamResponse;
import com.sp.api.stream.dto.UpdateStreamRequest;
import com.sp.api.stream.service.StreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StreamResponse>>> findAll() {
        List<StreamResponse> responses = streamService.findAll();

        return ResponseEntity.ok(
                new ApiResponse<>(true, responses)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamResponse>> findById(
            @PathVariable Long id
    ) {

        StreamResponse response = streamService.findById(id);

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StreamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStreamRequest request,
            Authentication authentication
    ) {

        StreamResponse response = streamService.update(
                id,
                request,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {

        streamService.delete(id, authentication.getName());

        return ResponseEntity.ok(
                new ApiResponse<>(true, null)
        );
    }
}