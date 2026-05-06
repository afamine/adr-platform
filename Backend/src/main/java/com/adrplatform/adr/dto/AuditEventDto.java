package com.adrplatform.adr.dto;

import java.util.Map;
import java.util.UUID;

public record AuditEventDto(
        UUID id,
        String type,
        String actor,
        String action,
        String timestamp,
        String detail,
        Map<String, Object> payload
) {
}
