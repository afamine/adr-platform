package com.adrplatform.auth.dto;

public record WorkspaceSlugStatus(
        String slug,
        boolean exists,
        String workspaceName,
        String joinPolicy,
        boolean canJoinBySlug,
        String message) {
}
