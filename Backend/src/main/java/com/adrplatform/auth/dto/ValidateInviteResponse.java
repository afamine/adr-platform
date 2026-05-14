package com.adrplatform.auth.dto;

public record ValidateInviteResponse(
    String email,
    String workspaceName,
    String role
) {}
