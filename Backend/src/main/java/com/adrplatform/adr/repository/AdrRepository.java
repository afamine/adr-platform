package com.adrplatform.adr.repository;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AdrStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdrRepository extends JpaRepository<Adr, UUID> {

    List<Adr> findAllByWorkspace_IdOrderByAdrNumberDesc(UUID workspaceId);

    Optional<Adr> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);

    @Query("select coalesce(max(a.adrNumber), 0) from Adr a where a.workspace.id = :workspaceId")
    int findMaxAdrNumber(@Param("workspaceId") UUID workspaceId);

    @Query("""
            select a from Adr a
            where a.workspace.id = :workspaceId
              and (:status is null or a.status = :status)
              and (
                    :search is null
                 or lower(a.title) like lower(concat('%', :search, '%'))
                 or lower(coalesce(a.context, '')) like lower(concat('%', :search, '%'))
                 or lower(coalesce(a.decision, '')) like lower(concat('%', :search, '%'))
                 or lower(coalesce(a.alternatives, '')) like lower(concat('%', :search, '%'))
              )
            order by a.adrNumber desc
            """)
    List<Adr> search(@Param("workspaceId") UUID workspaceId,
                     @Param("status") AdrStatus status,
                     @Param("search") String search);
}
