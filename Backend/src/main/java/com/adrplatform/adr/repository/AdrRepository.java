package com.adrplatform.adr.repository;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AdrStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdrRepository extends JpaRepository<Adr, UUID> {

    List<Adr> findAllByWorkspace_IdOrderByAdrNumberDesc(UUID workspaceId);

    @Query("SELECT COUNT(a) FROM Adr a WHERE a.workspace.id = :wsId")
    Long countByWorkspace(@Param("wsId") UUID wsId);

    @Query("SELECT COUNT(a) FROM Adr a WHERE a.workspace.id = :wsId AND a.createdAt >= :since")
    Long countSince(@Param("wsId") UUID wsId, @Param("since") Instant since);

    @Query("SELECT COUNT(a) FROM Adr a WHERE a.workspace.id = :wsId AND a.status = :status")
    Long countByStatus(@Param("wsId") UUID wsId, @Param("status") AdrStatus status);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400.0) FROM adr WHERE workspace_id = :wsId AND status IN ('ACCEPTED','REJECTED')", nativeQuery = true)
    Double avgReviewTimeDays(@Param("wsId") UUID wsId);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400.0) FROM adr WHERE workspace_id = :wsId AND status IN ('ACCEPTED','REJECTED') AND updated_at >= :from AND updated_at <= :to", nativeQuery = true)
    Double avgReviewTimeDaysBetween(@Param("wsId") UUID wsId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a.status, COUNT(a) FROM Adr a WHERE a.workspace.id = :wsId GROUP BY a.status")
    List<Object[]> countGroupByStatus(@Param("wsId") UUID wsId);

    @Query("SELECT COUNT(a) FROM Adr a WHERE a.workspace.id = :wsId AND a.createdAt >= :from AND a.createdAt <= :to")
    Long countCreatedBetween(@Param("wsId") UUID wsId, @Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT a FROM Adr a WHERE a.workspace.id = :wsId ORDER BY a.updatedAt DESC")
    List<Adr> findRecentByWorkspace(@Param("wsId") UUID wsId, Pageable pageable);

    Optional<Adr> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    @Query("select coalesce(max(a.adrNumber), 0) from Adr a where a.workspace.id = :workspaceId")
    int findMaxAdrNumber(@Param("workspaceId") UUID workspaceId);

    @Query(value = "SELECT next_adr_number(:workspaceId)", nativeQuery = true)
    int nextAdrNumber(@Param("workspaceId") UUID workspaceId);

    @Query(value = """
            SELECT * FROM adr a
            WHERE a.workspace_id = :workspaceId
              AND (CAST(:status AS text) IS NULL OR a.status = CAST(:status AS text))
              AND (
                    CAST(:search AS text) IS NULL
                 OR lower(a.title) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.context, '')) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.decision, '')) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.alternatives, '')) LIKE lower('%' || CAST(:search AS text) || '%')
              )
            ORDER BY a.adr_number DESC
            """, nativeQuery = true)
    List<Adr> search(@Param("workspaceId") UUID workspaceId,
                     @Param("status") String status,
                     @Param("search") String search);

    @Query(value = """
            SELECT * FROM adr a
            WHERE a.workspace_id = :workspaceId
              AND (CAST(:status AS text) IS NULL OR a.status = CAST(:status AS text))
              AND (
                    CAST(:search AS text) IS NULL
                 OR lower(a.title) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.context, '')) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.decision, '')) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.alternatives, '')) LIKE lower('%' || CAST(:search AS text) || '%')
              )
            """,
            countQuery = """
            SELECT COUNT(*) FROM adr a
            WHERE a.workspace_id = :workspaceId
              AND (CAST(:status AS text) IS NULL OR a.status = CAST(:status AS text))
              AND (
                    CAST(:search AS text) IS NULL
                 OR lower(a.title) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.context, '')) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.decision, '')) LIKE lower('%' || CAST(:search AS text) || '%')
                 OR lower(COALESCE(a.alternatives, '')) LIKE lower('%' || CAST(:search AS text) || '%')
              )
            """,
            nativeQuery = true)
    Page<Adr> searchPaged(@Param("workspaceId") UUID workspaceId,
                          @Param("status") String status,
                          @Param("search") String search,
                          Pageable pageable);
}
