package com.adrplatform.project.repository;

import com.adrplatform.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findAllByWorkspace_IdOrderByArchivedAscNameAsc(UUID workspaceId);
    Optional<Project> findByIdAndWorkspace_Id(UUID id, UUID workspaceId);
    boolean existsByWorkspace_IdAndNameIgnoreCase(UUID workspaceId, String name);
}
