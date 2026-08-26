package com.sp.api.block.controller;

import com.sp.api.block.dto.BlockResponse;
import com.sp.api.block.dto.BlockedUserResponse;
import com.sp.api.block.service.BlockService;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 로그인한 본인의 계정 자원이라 /api/users 아래에 둔다. 전부 인증이 필요하다. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<BlockResponse>> toggleBlock(
            @PathVariable Long userId,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                blockService.toggle(userId, authentication.getName())
        ));
    }

    @GetMapping("/me/blocks")
    public ResponseEntity<ApiResponse<PageResponse<BlockedUserResponse>>> myBlocks(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                blockService.findMyBlocks(authentication.getName(), pageable)
        ));
    }
}
