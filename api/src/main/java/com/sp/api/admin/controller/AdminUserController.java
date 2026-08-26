package com.sp.api.admin.controller;

import com.sp.api.admin.dto.AdminUserResponse;
import com.sp.api.admin.dto.ChangeRoleRequest;
import com.sp.api.admin.service.AdminUserService;
import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 사용자 권한 관리. 인가는 SecurityConfig 에서 ADMIN 으로 제한한다. */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> findAll(
            @RequestParam(required = false) User.Role role,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return ResponseEntity.ok(ApiResponse.ok(adminUserService.findAll(role, pageable)));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.changeRole(userId, request, authentication.getName())
        ));
    }
}
