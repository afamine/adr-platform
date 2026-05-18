package com.adrplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Full name cannot be empty.") @Size(max = 100) String fullName,
        @Size(max = 7) String avatarColor) {
}
