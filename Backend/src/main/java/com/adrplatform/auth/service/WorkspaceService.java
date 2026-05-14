package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.dto.UpdateWorkspaceRequest;
import com.adrplatform.auth.dto.WorkspaceDto;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public WorkspaceDto getCurrentWorkspace() {
        Workspace workspace = workspaceRepository.findById(tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        long memberCount = userRepository.countByWorkspace_Id(tenantContext.getWorkspaceId());
        return toDto(workspace, memberCount);
    }

    @Transactional
    public WorkspaceDto updateWorkspace(UpdateWorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findById(tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        String newSlug = request.slug().trim().toLowerCase();
        workspaceRepository.findBySlugAndIdNot(newSlug, workspace.getId()).ifPresent(other -> {
            throw new BadRequestException("Slug already taken.");
        });

        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String oldPayload = buildPayload(workspace);

        workspace.setName(request.name().trim());
        workspace.setSlug(newSlug);
        workspace.setVoteQuorum(request.voteQuorum());
        workspace.setQuorumMode(request.quorumMode());
        Workspace saved = workspaceRepository.save(workspace);

        auditService.record(actor, saved, "WORKSPACE_UPDATED", "WORKSPACE", saved.getId(),
                oldPayload, buildPayload(saved));

        log.info("Workspace {} updated by {}", saved.getId(), actor.getEmail());
        long memberCount = userRepository.countByWorkspace_Id(saved.getId());
        return toDto(saved, memberCount);
    }

    @Transactional
    public void resetWorkspace() {
        Workspace workspace = workspaceRepository.findById(tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String oldPayload = buildPayload(workspace);

        workspace.setVoteQuorum(2);
        workspace.setQuorumMode("AUTO");
        Workspace saved = workspaceRepository.save(workspace);

        auditService.record(actor, saved, "WORKSPACE_RESET", "WORKSPACE", saved.getId(),
                oldPayload, buildPayload(saved));

        log.info("Workspace {} reset to defaults by {}", saved.getId(), actor.getEmail());
    }

    private WorkspaceDto toDto(Workspace workspace, long memberCount) {
        return new WorkspaceDto(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getVoteQuorum(),
                workspace.getQuorumMode(),
                memberCount,
                workspace.getCreatedAt());
    }

    private String buildPayload(Workspace w) {
        return String.format(
                "{\"name\":\"%s\",\"slug\":\"%s\",\"voteQuorum\":%d,\"quorumMode\":\"%s\"}",
                w.getName(), w.getSlug(), w.getVoteQuorum(), w.getQuorumMode());
    }
}
