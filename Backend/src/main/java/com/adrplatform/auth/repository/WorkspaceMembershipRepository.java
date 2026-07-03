package com.adrplatform.auth.repository;

import com.adrplatform.auth.domain.WorkspaceMembership;
import com.adrplatform.auth.domain.WorkspaceMembershipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMembershipRepository extends JpaRepository<WorkspaceMembership, UUID> {

    @EntityGraph(attributePaths = {"workspace", "user"})
    Optional<WorkspaceMembership> findByUser_IdAndWorkspace_Id(UUID userId, UUID workspaceId);

    @EntityGraph(attributePaths = {"workspace"})
    List<WorkspaceMembership> findAllByUser_IdAndStatusOrderByCreatedAtAsc(UUID userId, WorkspaceMembershipStatus status);

    @EntityGraph(attributePaths = {"workspace", "user"})
    List<WorkspaceMembership> findAllByWorkspace_IdAndStatusOrderByCreatedAtAsc(UUID workspaceId, WorkspaceMembershipStatus status);

    long countByWorkspace_IdAndStatus(UUID workspaceId, WorkspaceMembershipStatus status);
}
