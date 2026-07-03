package com.adrplatform.auth.dto;

import com.adrplatform.auth.domain.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkspaceInvitationDto(
        UUID tokenId,
        UUID userId,
        String email,
        Role role,
        String status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}
