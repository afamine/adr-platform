package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.TokenType;
import com.adrplatform.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Query("delete from VerificationToken v where v.user.id = :userId and v.tokenType = :type and v.used = false")
    int deleteUnusedByUserAndType(@Param("userId") UUID userId, @Param("type") TokenType type);

    @Modifying
    @Query("delete from VerificationToken v where v.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
