package com.adrplatform.adr.dto;

import com.adrplatform.adr.domain.AdrStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StatusTransitionRequest(
        @NotNull AdrStatus status,
        UUID supersededByAdrId
) {}
