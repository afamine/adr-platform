package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.RefreshToken;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.exception.UnauthorizedException;
import com.adrplatform.auth.repository.RefreshTokenRepository;
import com.adrplatform.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    /**
     * Persists a refresh token for a user.
     */
    @Transactional
    public RefreshToken create(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryAt(jwtService.extractExpiration(token))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validates token existence, revocation state, and expiry.
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (storedToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token revoked");
        }

        if (storedToken.getExpiryAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        return storedToken;
    }

    /**
     * Revokes one refresh token.
     */
    @Transactional
    public void revoke(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Revokes all active refresh tokens for a user.
     */
    @Transactional
    public void revokeAllForUser(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId());
        tokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }

    @Transactional
    public long cleanupExpired() {
        return refreshTokenRepository.deleteByExpiryAtBefore(Instant.now());
    }
}
