package com.adrplatform.project.service;

import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.auth.service.AuditService;
import com.adrplatform.common.AuditActions;
import com.adrplatform.realtime.WorkspaceEventService;
import com.adrplatform.project.domain.Project;
import com.adrplatform.project.dto.CreateProjectRequest;
import com.adrplatform.project.dto.ProjectDto;
import com.adrplatform.project.dto.UpdateProjectRequest;
import com.adrplatform.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final AdrRepository adrRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final WorkspaceEventService workspaceEventService;

    @Transactional(readOnly = true)
    public List<ProjectDto> listProjects() {
        return projectRepository.findAllByWorkspace_IdOrderByArchivedAscNameAsc(workspaceId()).stream()
                .map(ProjectDto::fromEntity).toList();
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request) {
        User actor = adminActor();
        String name = request.name().trim();
        if (projectRepository.existsByWorkspace_IdAndNameIgnoreCase(workspaceId(), name)) {
            throw new BadRequestException("A project with this name already exists in this workspace.");
        }
        Project project = projectRepository.save(Project.builder().workspace(actor.getWorkspace())
                .name(name).description(blankToNull(request.description())).build());
        auditService.record(actor, actor.getWorkspace(), AuditActions.PROJECT_CREATED, "PROJECT", project.getId(), null, null);
        workspaceEventService.publishToWorkspace(workspaceId(), "PROJECT_UPDATED", null);
        return ProjectDto.fromEntity(project);
    }

    @Transactional
    public ProjectDto updateProject(UUID id, UpdateProjectRequest request) {
        User actor = adminActor();
        Project project = projectForWorkspace(id);
        String name = request.name().trim();
        if (!project.getName().equalsIgnoreCase(name)
                && projectRepository.existsByWorkspace_IdAndNameIgnoreCase(workspaceId(), name)) {
            throw new BadRequestException("A project with this name already exists in this workspace.");
        }
        project.setName(name);
        project.setDescription(blankToNull(request.description()));
        Project saved = projectRepository.save(project);
        auditService.record(actor, actor.getWorkspace(), AuditActions.PROJECT_UPDATED, "PROJECT", saved.getId(), null, null);
        workspaceEventService.publishToWorkspace(workspaceId(), "PROJECT_UPDATED", null);
        return ProjectDto.fromEntity(saved);
    }

    @Transactional
    public ProjectDto archiveProject(UUID id) {
        User actor = adminActor();
        Project project = projectForWorkspace(id);
        if (!project.isArchived()) {
            project.setArchived(true);
            adrRepository.clearProjectByProjectId(project.getId());
            projectRepository.save(project);
            auditService.record(actor, actor.getWorkspace(), AuditActions.PROJECT_ARCHIVED, "PROJECT", project.getId(), null, null);
        }
        workspaceEventService.publishToWorkspace(workspaceId(), "PROJECT_UPDATED", null);
        return ProjectDto.fromEntity(project);
    }

    private User adminActor() {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (actor.getRole() != Role.ADMIN) throw new AccessDeniedException("Only workspace administrators can manage projects.");
        return actor;
    }

    private Project projectForWorkspace(UUID id) {
        return projectRepository.findByIdAndWorkspace_Id(id, workspaceId())
                .orElseThrow(() -> new BadRequestException("Project not found in this workspace."));
    }

    private UUID workspaceId() {
        UUID id = tenantContext.getWorkspaceId();
        if (id == null) throw new BadRequestException("Workspace context not available.");
        return id;
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
