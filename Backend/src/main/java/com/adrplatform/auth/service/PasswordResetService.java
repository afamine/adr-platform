package com.adrplatform.auth.service;

import com.adrplatform.auth.config.PasswordResetProperties;
import com.adrplatform.auth.domain.PasswordResetToken;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.dto.ForgotPasswordRequest;
import com.adrplatform.auth.dto.ResetPasswordRequest;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.repository.PasswordResetTokenRepository;
import com.adrplatform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordResetProperties passwordResetProperties;
    private final MailService mailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void createResetToken(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null) {
            return;
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(user -> {
            passwordResetTokenRepository.deleteAllByUser_Id(user.getId());
            String rawToken = generateRawToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(hashToken(rawToken))
                    .expiresAt(Instant.now().plus(passwordResetProperties.getTokenTtl()))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(token);
            sendResetEmail(user, rawToken, passwordResetProperties.getTokenTtl());
            log.info("Password reset token generated for user {}", normalizedEmail);
        }, () -> log.debug("Password reset requested for non-existent email {}", normalizedEmail));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        passwordPolicyValidator.validate(request.getNewPassword());
        PasswordResetToken token = validateToken(request.getToken());
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        token.setUsed(true);
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        userRepository.save(user);
        passwordResetTokenRepository.deleteAllByUser_Id(user.getId());
        log.info("Password reset completed for user {}", user.getEmail());
    }

    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Reset token is required");
        }
        String tokenHash = hashToken(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid reset token"));

        if (token.isUsed()) {
            throw new BadRequestException("Reset token already used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token expired");
        }
        return token;
    }

    private void sendResetEmail(User user, String rawToken, Duration ttl) {
        String link = passwordResetProperties.getFrontendUrl() + "?token=" + rawToken;
        long minutes = ttl.toMinutes();
        String body = "Hello " + user.getFullName() + ",\n\n" +
                "We received a request to reset your ADR Platform password. " +
                "Use the link below to set a new password.\n\n" +
                link + "\n\n" +
                "This link will expire in " + minutes + " minutes.";
        mailService.sendPlainText(
                passwordResetProperties.getEmailFrom(),
                user.getEmail(),
                passwordResetProperties.getEmailSubject(),
                body
        );
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }
}
