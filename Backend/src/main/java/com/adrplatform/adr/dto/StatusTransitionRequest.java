package com.adrplatform.adr.dto;

import com.adrplatform.adr.domain.AdrStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(@NotNull AdrStatus status) {}
