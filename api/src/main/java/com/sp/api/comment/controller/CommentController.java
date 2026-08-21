package com.sp.api.comment.controller;

import com.sp.api.comment.dto.CommentResponse;
import com.sp.api.comment.dto.CreateCommentRequest;
import com.sp.api.comment.dto.UpdateCommentRequest;
import com.sp.api.comment.service.CommentService;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streams/{streamId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @PathVariable Long streamId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {

        CommentResponse response = commentService.create(
                streamId, request, authentication.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> findByStream(
            @PathVariable Long streamId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(commentService.findByStream(streamId, pageable))
        );
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(
            @PathVariable Long streamId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication
    ) {

        CommentResponse response = commentService.update(
                streamId, commentId, request, authentication.getName()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long streamId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {

        commentService.delete(streamId, commentId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
