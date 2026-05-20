package com.adrplatform.adr.dto;

public record HistoryEventDto(
        String actor,
        String action,
        String timeAgo,
        String eventType
) {}
