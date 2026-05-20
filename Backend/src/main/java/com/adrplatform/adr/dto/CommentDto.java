package com.adrplatform.adr.dto;

import java.util.UUID;

public record CommentDto(
        UUID id,
        UUID adrId,
        UUID authorId,
        String authorName,
        String authorInitials,
        String content,
        boolean resolved,
        String createdAt,
        String updatedAt
) {}
