package com.adrplatform.auth.dto;

public record InviteUserResponse(
        String message,
        String inviteLink,
        WorkspaceInvitationDto invitation
) {
}
