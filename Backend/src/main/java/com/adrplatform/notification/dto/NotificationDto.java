package com.adrplatform.notification.dto;

import java.util.UUID;

public record NotificationDto(
        UUID id,
        String type,
        String title,
        String body,
        UUID adrId,
        boolean isRead,
        String createdAt,
        String timeAgo) {
}
