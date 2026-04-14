package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBefore(Instant instant);

    long deleteAllByUser_Id(UUID userId);
}
