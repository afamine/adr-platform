package com.adrplatform.auth.security;

import com.adrplatform.auth.domain.BlacklistedToken;
import com.adrplatform.auth.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public void blacklist(String token) {
        String hash = sha256(token);
        if (!blacklistedTokenRepository.existsByTokenHash(hash)) {
            Instant expiresAt = safeExtractExpiration(token);
            blacklistedTokenRepository.save(BlacklistedToken.builder()
                    .tokenHash(hash)
                    .expiresAt(expiresAt)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String token) {
        return blacklistedTokenRepository.existsByTokenHash(sha256(token));
    }

    @Transactional
    public long cleanupExpired() {
        long removed = blacklistedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) log.info("Cleaned up {} expired blacklisted tokens", removed);
        return removed;
    }

    private Instant safeExtractExpiration(String token) {
        try {
            return jwtService.extractExpiration(token);
        } catch (Exception ex) {
            return Instant.now().plusSeconds(900);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
