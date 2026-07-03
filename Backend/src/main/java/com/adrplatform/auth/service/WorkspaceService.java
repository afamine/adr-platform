package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.domain.WorkspaceMembership;
import com.adrplatform.auth.domain.WorkspaceMembershipStatus;
import com.adrplatform.auth.dto.AuthResponse;
import com.adrplatform.auth.dto.UpdateWorkspaceRequest;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.dto.WorkspaceDto;
import com.adrplatform.auth.dto.WorkspaceMembershipDto;
import com.adrplatform.auth.dto.WorkspaceSlugStatus;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.common.AuditActions;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.repository.WorkspaceMembershipRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.JwtService;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public WorkspaceDto getCurrentWorkspace() {
        Workspace workspace = workspaceRepository.findById(tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        long memberCount = workspaceMembershipRepository.countByWorkspace_IdAndStatus(
                tenantContext.getWorkspaceId(), WorkspaceMembershipStatus.ACTIVE);
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
        workspace.setJoinPolicy(request.joinPolicy());
        Workspace saved = workspaceRepository.save(workspace);

        auditService.record(actor, saved, AuditActions.WORKSPACE_UPDATED, "WORKSPACE", saved.getId(),
                oldPayload, buildPayload(saved));

        log.info("Workspace {} updated by {}", saved.getId(), actor.getEmail());
        long memberCount = workspaceMembershipRepository.countByWorkspace_IdAndStatus(
                saved.getId(), WorkspaceMembershipStatus.ACTIVE);
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
        workspace.setJoinPolicy("INVITE_ONLY");
        Workspace saved = workspaceRepository.save(workspace);

        auditService.record(actor, saved, AuditActions.WORKSPACE_RESET, "WORKSPACE", saved.getId(),
                oldPayload, buildPayload(saved));

        log.info("Workspace {} reset to defaults by {}", saved.getId(), actor.getEmail());
    }

    @Transactional(readOnly = true)
    public WorkspaceSlugStatus getSlugStatus(String rawSlug) {
        String slug = rawSlug == null ? "" : rawSlug.trim().toLowerCase();
        if (slug.isBlank() || !slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$")) {
            return new WorkspaceSlugStatus(
                    slug,
                    false,
                    null,
                    null,
                    false,
                    "Enter a valid lowercase workspace slug.");
        }

        return workspaceRepository.findBySlug(slug)
                .map(workspace -> {
                    boolean canJoin = "ALLOW_SLUG".equals(workspace.getJoinPolicy());
                    String message = canJoin
                            ? "Workspace found. You can join this team."
                            : "Workspace found, but joining requires an invitation.";
                    return new WorkspaceSlugStatus(
                            slug,
                            true,
                            workspace.getName(),
                            workspace.getJoinPolicy(),
                            canJoin,
                            message);
                })
                .orElseGet(() -> new WorkspaceSlugStatus(
                        slug,
                        false,
                        null,
                        null,
                        false,
                        "No workspace found with this slug."));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMembershipDto> listMyWorkspaces() {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID currentWorkspaceId = tenantContext.getWorkspaceId();
        return workspaceMembershipRepository.findAllByUser_IdAndStatusOrderByCreatedAtAsc(
                        actor.getId(), WorkspaceMembershipStatus.ACTIVE)
                .stream()
                .map(membership -> WorkspaceMembershipDto.fromEntity(membership, currentWorkspaceId))
                .toList();
    }

    @Transactional
    public AuthResponse switchWorkspace(UUID workspaceId) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WorkspaceMembership membership = workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(actor.getId(), workspaceId)
                .filter(m -> m.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace membership not found."));

        User user = userRepository.findByIdWithWorkspace(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setWorkspace(membership.getWorkspace());
        user.setRole(membership.getRole());
        User saved = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(saved);
        String refreshToken = jwtService.generateRefreshToken(saved);
        refreshTokenService.create(saved, refreshToken);

        log.info("User {} switched to workspace {}", saved.getEmail(), membership.getWorkspace().getSlug());
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.fromEntity(saved))
                .requiresTwoFactorSetup(saved.isTotpSetupRequired() && !saved.isTotpEnabled())
                .build();
    }

    private WorkspaceDto toDto(Workspace workspace, long memberCount) {
        return new WorkspaceDto(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getVoteQuorum(),
                workspace.getQuorumMode(),
                workspace.getJoinPolicy(),
                memberCount,
                workspace.getCreatedAt());
    }

    private String buildPayload(Workspace w) {
        return String.format(
                "{\"name\":\"%s\",\"slug\":\"%s\",\"voteQuorum\":%d,\"quorumMode\":\"%s\",\"joinPolicy\":\"%s\"}",
                w.getName(), w.getSlug(), w.getVoteQuorum(), w.getQuorumMode(), w.getJoinPolicy());
    }
}
