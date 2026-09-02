package com.adrplatform.realtime;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceEvent(
        String type,
        UUID workspaceId,
        UUID adrId,
        Instant occurredAt
) {
    public static WorkspaceEvent of(String type, UUID workspaceId, UUID adrId) {
        return new WorkspaceEvent(type, workspaceId, adrId, Instant.now());
    }
}