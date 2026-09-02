package com.adrplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Request to begin a verified email-address change for the current account. */
public record EmailChangeRequest(
        @NotBlank(message = "New email is required.")
        @Email(message = "Enter a valid email address.") String newEmail,
        @NotBlank(message = "Current password is required.") String currentPassword,
        @Pattern(regexp = "^$|^\\d{6}$", message = "Enter a valid six-digit verification code.") String totpCode) {
}
