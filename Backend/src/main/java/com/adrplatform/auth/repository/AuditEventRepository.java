package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.AuditEvent;
import com.adrplatform.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    @Query("""
            select ae from AuditEvent ae
            left join fetch ae.user
            where ae.workspace.id = :workspaceId
              and ae.entityType = 'ADR'
              and ae.entityId = :adrId
            order by ae.createdAt desc
            """)
    List<AuditEvent> findAdrAuditEvents(@Param("workspaceId") UUID workspaceId, @Param("adrId") UUID adrId);

    @Query(value = """
            SELECT ae FROM AuditEvent ae
            LEFT JOIN FETCH ae.user
            WHERE ae.workspace.id = :workspaceId
              AND (CAST(:actorId AS java.util.UUID) IS NULL OR ae.user.id = :actorId)
              AND (CAST(:action AS string) IS NULL OR ae.action = :action)
              AND (CAST(:from AS java.time.Instant) IS NULL OR ae.createdAt >= :from)
              AND (CAST(:to AS java.time.Instant) IS NULL OR ae.createdAt <= :to)
            ORDER BY ae.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(ae) FROM AuditEvent ae
            WHERE ae.workspace.id = :workspaceId
              AND (CAST(:actorId AS java.util.UUID) IS NULL OR ae.user.id = :actorId)
              AND (CAST(:action AS string) IS NULL OR ae.action = :action)
              AND (CAST(:from AS java.time.Instant) IS NULL OR ae.createdAt >= :from)
              AND (CAST(:to AS java.time.Instant) IS NULL OR ae.createdAt <= :to)
            """)
    Page<AuditEvent> findWorkspaceAuditEvents(
            @Param("workspaceId") UUID workspaceId,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            SELECT COUNT(ae) FROM AuditEvent ae
            WHERE ae.workspace.id = :workspaceId
              AND (CAST(:actorId AS java.util.UUID) IS NULL OR ae.user.id = :actorId)
              AND (CAST(:action AS string) IS NULL OR ae.action = :action)
              AND (CAST(:from AS java.time.Instant) IS NULL OR ae.createdAt >= :from)
              AND (CAST(:to AS java.time.Instant) IS NULL OR ae.createdAt <= :to)
            """)
    long countWorkspaceAuditEvents(
            @Param("workspaceId") UUID workspaceId,
            @Param("actorId") UUID actorId,
            @Param("action") String action,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            SELECT DISTINCT ae.user FROM AuditEvent ae
            WHERE ae.workspace.id = :workspaceId
              AND ae.user IS NOT NULL
            """)
    List<User> findActorsInWorkspace(@Param("workspaceId") UUID workspaceId);
}
