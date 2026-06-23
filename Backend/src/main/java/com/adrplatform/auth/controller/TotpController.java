package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.TotpDisableRequest;
import com.adrplatform.auth.dto.TotpEnableRequest;
import com.adrplatform.auth.dto.TotpValidateRequest;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.dto.AuthResponse;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.security.JwtService;
import com.adrplatform.auth.service.AuthService;
import com.adrplatform.auth.service.SecretEncryptionService;
import com.adrplatform.auth.service.TotpService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Authentication", description = "TOTP 2FA setup and verification endpoints")
public class TotpController {

    private final TotpService totpService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final SecretEncryptionService secretEncryptionService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getStatus(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return ResponseEntity.ok(Map.of("enabled", user.isTotpEnabled()));
    }

    @GetMapping("/setup")
    public ResponseEntity<Map<String, String>> setup(Authentication auth) {
        User user = (User) auth.getPrincipal();
        String secret = totpService.generateSecret();
        String qrBase64 = totpService.generateQrCodeBase64(user.getEmail(), secret);
        user.setTotpPendingSecret(secretEncryptionService.encrypt(secret));
        user.setTotpPendingExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
            "qrCodeBase64", qrBase64,
            "secret", formatSecret(secret)
        ));
    }

    @PostMapping("/enable")
    public ResponseEntity<Map<String, String>> enable(
            @RequestBody @Valid TotpEnableRequest request,
            Authentication auth) {
        User user = (User) auth.getPrincipal();
        if (user.getTotpPendingSecret() == null
                || user.getTotpPendingExpiresAt() == null
                || user.getTotpPendingExpiresAt().isBefore(Instant.now())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "2FA setup expired. Please restart setup."));
        }

        String pendingSecret = secretEncryptionService.decrypt(user.getTotpPendingSecret());
        if (!totpService.verifyCode(pendingSecret, request.code())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid code. Please check your authenticator app."));
        }
        user.setTotpSecret(secretEncryptionService.encrypt(pendingSecret));
        user.setTotpPendingSecret(null);
        user.setTotpPendingExpiresAt(null);
        user.setTotpEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "2FA enabled successfully"));
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disable(
            @RequestBody @Valid TotpDisableRequest request,
            Authentication auth) {
        User user = (User) auth.getPrincipal();
        if (!user.isTotpEnabled() || user.getTotpSecret() == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "2FA is not enabled on this account."));
        }
        String storedSecret = secretEncryptionService.decrypt(user.getTotpSecret());
        if (!totpService.verifyCode(storedSecret, request.code())) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid code. Please check your authenticator app."));
        }
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "2FA has been disabled."));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody @Valid TotpValidateRequest request) {
        try {
            Claims claims = jwtService.parseToken(request.pendingToken());
            if (!jwtService.isPending2faToken(claims)) {
                return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid session token."));
            }
            UUID userId = UUID.fromString(claims.getSubject());
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isActive() || !user.isTotpEnabled() || user.getTotpSecret() == null) {
                return ResponseEntity.status(401)
                    .body(Map.of("message", "2FA is not enabled for this account."));
            }

            String storedSecret = secretEncryptionService.decrypt(user.getTotpSecret());
            if (!totpService.verifyCode(storedSecret, request.code())) {
                return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid code. Please try again."));
            }
            AuthResponse fullResponse = authService.buildAuthResponse(user);
            return ResponseEntity.ok(fullResponse);
        } catch (Exception e) {
            log.error("2FA validation failed: {}", e.getMessage());
            return ResponseEntity.status(401)
                .body(Map.of("message", "Authentication failed. Please log in again."));
        }
    }

    private String formatSecret(String secret) {
        return secret.replaceAll("(.{4})(?!$)", "$1 ");
    }
}

