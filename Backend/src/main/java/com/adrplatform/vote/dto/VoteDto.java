package com.adrplatform.vote.dto;

import com.adrplatform.vote.domain.VoteType;

import java.time.LocalDateTime;
import java.util.UUID;

public record VoteDto(
        UUID id,
        UUID adrId,
        UUID voterId,
        String voterName,
        String voterRole,
        VoteType voteType,
        String comment,
        LocalDateTime createdAt
) {
}
