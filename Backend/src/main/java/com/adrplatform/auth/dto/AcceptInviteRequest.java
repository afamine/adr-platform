package com.adrplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
    @NotBlank String token,
    @NotBlank @Size(max = 100) String fullName,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String confirmPassword
) {}
