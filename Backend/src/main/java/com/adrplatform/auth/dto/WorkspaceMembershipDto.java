package com.adrplatform.auth.dto;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.WorkspaceMembership;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMembershipDto(
        UUID workspaceId,
        String workspaceName,
        String workspaceSlug,
        Role role,
        boolean current,
        Instant joinedAt
) {
    public static WorkspaceMembershipDto fromEntity(WorkspaceMembership membership, UUID currentWorkspaceId) {
        return new WorkspaceMembershipDto(
                membership.getWorkspace().getId(),
                membership.getWorkspace().getName(),
                membership.getWorkspace().getSlug(),
                membership.getRole(),
                membership.getWorkspace().getId().equals(currentWorkspaceId),
                membership.getAcceptedAt() != null ? membership.getAcceptedAt() : membership.getCreatedAt()
        );
    }
}
