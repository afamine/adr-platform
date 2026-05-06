package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
