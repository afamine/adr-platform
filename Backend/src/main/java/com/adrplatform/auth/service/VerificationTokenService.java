package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.TokenType;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.VerificationToken;
import com.adrplatform.auth.exception.InvalidTokenException;
import com.adrplatform.auth.exception.TokenExpiredException;
import com.adrplatform.auth.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepository;

    /**
     * Creates a new token for the user, invalidating any previous unused token of the same type.
     */
    @Transactional
    public String createToken(User user, TokenType type, int expiryHours) {
        verificationTokenRepository.deleteUnusedByUserAndType(user.getId(), type);

        String tokenValue = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        VerificationToken vt = VerificationToken.builder()
                .user(user)
                .token(tokenValue)
                .tokenType(type)
                .expiresAt(LocalDateTime.now().plusHours(expiryHours))
                .used(false)
                .build();

        verificationTokenRepository.save(vt);
        return tokenValue;
    }

    /**
     * Validates a raw token string. Marks it as used on success.
     * Throws TokenExpiredException (errorType=EXPIRED) or InvalidTokenException (errorType=INVALID).
     */
    @Transactional
    public VerificationToken validateAndConsume(String tokenString, TokenType expectedType) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new InvalidTokenException("This verification link is invalid or has already been used.");
        }

        VerificationToken token = verificationTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new InvalidTokenException("This verification link is invalid or has already been used."));

        if (token.getTokenType() != expectedType) {
            throw new InvalidTokenException("This verification link is invalid or has already been used.");
        }
        if (token.isUsed()) {
            throw new InvalidTokenException("This verification link is invalid or has already been used.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("This verification link has expired.");
        }

        token.setUsed(true);
        verificationTokenRepository.save(token);
        return token;
    }

    /**
     * Daily cleanup of expired tokens at 03:00.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int removed = verificationTokenRepository.deleteExpiredBefore(LocalDateTime.now());
        if (removed > 0) {
            log.info("Cleaned up {} expired verification tokens", removed);
        }
    }
}
