package com.adrplatform.adr.dto;

import java.util.UUID;

public record TeamMemberDto(
        UUID id,
        String fullName,
        String initials,
        String role,
        String avatarColor
) {}
