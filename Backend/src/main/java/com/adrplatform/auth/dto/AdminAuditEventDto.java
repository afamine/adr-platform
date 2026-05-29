package com.adrplatform.auth.dto;

import java.util.UUID;

public record AdminAuditEventDto(
        UUID id,
        String timestamp,
        String actorId,
        String actorName,
        String actorEmail,
        String actorInitials,
        String action,
        String actionLabel,
        String entityType,
        String entityId,
        String entityLabel,
        String detail,
        String oldValueJson,
        String newValueJson,
        String ipAddress
) {}
