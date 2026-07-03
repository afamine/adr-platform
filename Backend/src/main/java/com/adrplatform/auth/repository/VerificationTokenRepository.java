package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.TokenType;
import com.adrplatform.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    @Query("select v from VerificationToken v join fetch v.user u join fetch u.workspace left join fetch v.workspace w " +
            "where w.id = :workspaceId and v.tokenType = :type order by v.createdAt desc")
    List<VerificationToken> findAllWorkspaceTokens(@Param("workspaceId") UUID workspaceId, @Param("type") TokenType type);

    @Modifying
    @Query("delete from VerificationToken v where v.user.id = :userId and v.tokenType = :type and v.used = false")
    int deleteUnusedByUserAndType(@Param("userId") UUID userId, @Param("type") TokenType type);

    @Modifying
    @Query("delete from VerificationToken v where v.user.id = :userId and v.workspace.id = :workspaceId and v.tokenType = :type and v.used = false")
    int deleteUnusedByUserAndWorkspaceAndType(@Param("userId") UUID userId, @Param("workspaceId") UUID workspaceId, @Param("type") TokenType type);

    @Modifying
    @Query("delete from VerificationToken v where v.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
