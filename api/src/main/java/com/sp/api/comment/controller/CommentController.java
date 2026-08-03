package com.sp.api.comment.controller;

import com.sp.api.comment.dto.CommentResponse;
import com.sp.api.comment.dto.CreateCommentRequest;
import com.sp.api.comment.dto.UpdateCommentRequest;
import com.sp.api.comment.service.CommentService;
import com.sp.api.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                streamId,
                request,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> findByStream(
            @PathVariable Long streamId
    ) {

        List<CommentResponse> responses =
                commentService.findByStream(streamId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, responses)
        );
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            Authentication authentication
    ) {

        CommentResponse response = commentService.update(
                commentId,
                request,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, response)
        );
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long commentId,
            Authentication authentication
    ) {

        commentService.delete(
                commentId,
                authentication.getName()
        );

        return ResponseEntity.ok(
                new ApiResponse<>(true, null)
        );
    }
}