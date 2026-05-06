package com.adrplatform.vote.dto;

import com.adrplatform.vote.domain.VoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CastVoteRequest(
        @NotNull VoteType voteType,
        @NotBlank @Size(max = 500) String comment
) {
}
