package com.adrplatform.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug may only contain lowercase letters, digits, and hyphens.") String slug,
        @NotNull @Min(1) @Max(10) Integer voteQuorum,
        @NotBlank @Pattern(regexp = "^(AUTO|MANUAL)$", message = "quorumMode must be AUTO or MANUAL.") String quorumMode) {
}
