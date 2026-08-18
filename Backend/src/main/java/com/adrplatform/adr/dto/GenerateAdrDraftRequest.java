package com.adrplatform.adr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A short author-provided description used to generate an unsaved ADR draft. */
public record GenerateAdrDraftRequest(
        @NotBlank(message = "Problem description is required.")
        @Size(min = 20, max = 2000, message = "Problem description must be between 20 and 2,000 characters.")
        String problemDescription
) {
}
