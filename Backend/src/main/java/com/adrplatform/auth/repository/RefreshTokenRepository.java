package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(UUID userId);

    long deleteByExpiryAtBefore(Instant instant);
}
