package com.adrplatform.notification.controller;

import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.dto.MessageResponse;
import com.adrplatform.notification.dto.MarkAllReadResponse;
import com.adrplatform.notification.dto.NotificationDto;
import com.adrplatform.notification.dto.UnreadCountDto;
import com.adrplatform.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "User notification endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get notifications for the current user")
    @ApiResponse(responseCode = "200", description = "Notifications returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getNotifications(currentUserId(), limit, unreadOnly));
    }

    @Operation(summary = "Get unread notification count")
    @ApiResponse(responseCode = "200", description = "Count returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> getUnreadCount() {
        return ResponseEntity.ok(new UnreadCountDto(notificationService.getUnreadCount(currentUserId())));
    }

    @Operation(summary = "Mark a single notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PatchMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markRead(@PathVariable UUID id) {
        notificationService.markRead(id, currentUserId());
        return ResponseEntity.ok(MessageResponse.builder().message("Notification marked as read.").build());
    }

    @Operation(summary = "Mark all notifications as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PatchMapping("/mark-all-read")
    public ResponseEntity<MarkAllReadResponse> markAllRead() {
        long count = notificationService.markAllRead(currentUserId());
        return ResponseEntity.ok(new MarkAllReadResponse("All notifications marked as read.", count));
    }

    private UUID currentUserId() {
        return ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getId();
    }
}
