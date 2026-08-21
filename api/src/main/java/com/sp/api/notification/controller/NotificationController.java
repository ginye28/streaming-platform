package com.sp.api.notification.controller;

import com.sp.api.common.response.ApiResponse;
import com.sp.api.common.response.PageResponse;
import com.sp.api.notification.dto.NotificationResponse;
import com.sp.api.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> findMine(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {

        return ResponseEntity.ok(ApiResponse.ok(
                notificationService.findMine(authentication.getName(), pageable)
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(Authentication authentication) {

        long count = notificationService.unreadCount(authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {

        notificationService.markAsRead(notificationId, authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(Authentication authentication) {

        int updated = notificationService.markAllAsRead(authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(Map.of("updated", updated)));
    }
}
