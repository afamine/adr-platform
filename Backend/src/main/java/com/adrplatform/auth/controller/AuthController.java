package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.AcceptInviteRequest;
import com.adrplatform.auth.dto.AuthResponse;
import com.adrplatform.auth.dto.ChangePasswordRequest;
import com.adrplatform.auth.dto.ForgotPasswordRequest;
import com.adrplatform.auth.dto.LoginRequest;
import com.adrplatform.auth.dto.MessageResponse;
import com.adrplatform.auth.dto.RefreshRequest;
import com.adrplatform.auth.dto.RegisterRequest;
import com.adrplatform.auth.dto.RegisterResponse;
import com.adrplatform.auth.dto.ResendVerificationRequest;
import com.adrplatform.auth.dto.ResetPasswordRequest;
import com.adrplatform.auth.dto.ValidateInviteResponse;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.service.AuthService;
import com.adrplatform.auth.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Registration, login, token refresh, password management, and invitation endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Register a new user and create their workspace")
    @ApiResponse(responseCode = "200", description = "User registered — awaiting email verification")
    @ApiResponse(responseCode = "400", description = "Validation failed or password policy violation")
    @ApiResponse(responseCode = "409", description = "Email already registered or workspace slug already taken")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Login and get tokens")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Rotate refresh token")
    @ApiResponse(responseCode = "200", description = "Token refreshed")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Logout and invalidate active tokens")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Request password reset email")
    @ApiResponse(responseCode = "200", description = "Reset email dispatched")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.createResetToken(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reset password using the provided token")
    @ApiResponse(responseCode = "200", description = "Password reset successful")
    @ApiResponse(responseCode = "400", description = "Invalid request or token")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Verify the user email address using the provided token")
    @ApiResponse(responseCode = "200", description = "Email verified successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token (errorType=INVALID|EXPIRED)")
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @Operation(summary = "Re-send the email verification link")
    @ApiResponse(responseCode = "200", description = "Verification link dispatched (or silently ignored for unknown emails)")
    @ApiResponse(responseCode = "400", description = "Email is already verified")
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request));
    }

    @Operation(summary = "Validate a workspace invite token without consuming it")
    @ApiResponse(responseCode = "200", description = "Token is valid")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token (errorType=INVALID|EXPIRED)")
    @GetMapping("/validate-invite")
    public ResponseEntity<ValidateInviteResponse> validateInvite(@RequestParam("token") String token) {
        return ResponseEntity.ok(authService.validateInviteToken(token));
    }

    @Operation(summary = "Accept workspace invite and activate account")
    @ApiResponse(responseCode = "200", description = "Account activated and tokens returned")
    @ApiResponse(responseCode = "400", description = "Invalid token, expired token, or validation error")
    @PostMapping("/accept-invite")
    public ResponseEntity<AuthResponse> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        return ResponseEntity.ok(authService.acceptInvite(request));
    }

    @Operation(summary = "Sign out from all devices by revoking all refresh tokens")
    @ApiResponse(responseCode = "200", description = "All sessions revoked")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll() {
        return ResponseEntity.ok(authService.logoutAllDevices());
    }

    @PutMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password for authenticated user",
        responses = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or wrong current password"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        })
    public ResponseEntity<MessageResponse> changePassword(
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = (User) org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        authService.changePassword(request, user.getId());
        return ResponseEntity.ok(MessageResponse.builder()
            .message("Password updated successfully. Please log in again.")
            .build());
    }
}
