package com.adrplatform.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceDto(
        UUID id,
        String name,
        String slug,
        Integer voteQuorum,
        String quorumMode,
        Long memberCount,
        Instant createdAt) {
}
