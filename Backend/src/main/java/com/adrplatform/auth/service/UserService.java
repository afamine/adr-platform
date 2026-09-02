package com.adrplatform.auth.service;

import com.adrplatform.auth.config.AppProperties;
import com.adrplatform.auth.domain.NotificationPreferences;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.TokenType;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.domain.WorkspaceMembership;
import com.adrplatform.auth.domain.WorkspaceMembershipStatus;
import com.adrplatform.auth.domain.VerificationToken;
import com.adrplatform.auth.dto.InviteUserResponse;
import com.adrplatform.auth.dto.NotificationPreferencesDto;
import com.adrplatform.auth.dto.UpdatePreferencesRequest;
import com.adrplatform.auth.dto.UpdateProfileRequest;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.dto.WorkspaceInvitationDto;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.exception.ConflictException;
import com.adrplatform.common.AuditActions;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.repository.NotificationPreferencesRepository;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.repository.VerificationTokenRepository;
import com.adrplatform.auth.repository.WorkspaceMembershipRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final VerificationTokenService verificationTokenService;
    private final RefreshTokenService refreshTokenService;
    private final MailService mailService;
    private final AppProperties appProperties;

    /**
     * Returns current authenticated user profile.
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(principal.getId(), tenantContext.getWorkspaceId())
                .filter(m -> m.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                .map(UserDto::fromMembership)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Updates the display name of the currently authenticated user.
     */
    @Transactional
    public UserDto updateMyProfile(UpdateProfileRequest request) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WorkspaceMembership membership = workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(actor.getId(), tenantContext.getWorkspaceId())
                .filter(m -> m.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        User user = membership.getUser();

        String oldName = user.getFullName();
        user.setFullName(request.fullName().trim());
        if (request.avatarColor() != null && !request.avatarColor().isBlank()) {
            user.setAvatarColor(request.avatarColor());
        }
        User saved = userRepository.save(user);

        auditService.record(actor, actor.getWorkspace(), AuditActions.PROFILE_UPDATED, "USER", saved.getId(),
                "{\"fullName\":\"" + oldName + "\"}",
                "{\"fullName\":\"" + saved.getFullName() + "\"}");

        log.info("Profile updated for user {}", saved.getEmail());
        membership.setUser(saved);
        return UserDto.fromMembership(membership);
    }

    /**
     * Returns notification preferences for the current user, creating defaults on first access.
     */
    @Transactional
    public NotificationPreferencesDto getMyPreferences() {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        NotificationPreferences prefs = notificationPreferencesRepository.findByUser_Id(actor.getId())
                .orElseGet(() -> createDefaultPreferences(actor));
        return toPreferencesDto(prefs);
    }

    /**
     * Saves notification preferences for the current user.
     */
    @Transactional
    public NotificationPreferencesDto updateMyPreferences(UpdatePreferencesRequest request) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        NotificationPreferences prefs = notificationPreferencesRepository.findByUser_Id(actor.getId())
                .orElseGet(() -> createDefaultPreferences(actor));

        prefs.setEmailOnReview(request.emailOnReview());
        prefs.setEmailOnVote(request.emailOnVote());
        prefs.setEmailOnStatus(request.emailOnStatus());
        prefs.setSlackEnabled(request.slackEnabled());
        prefs.setSlackWebhook(request.slackWebhook());
        NotificationPreferences saved = notificationPreferencesRepository.save(prefs);
        return toPreferencesDto(saved);
    }

    private NotificationPreferences createDefaultPreferences(User user) {
        NotificationPreferences prefs = NotificationPreferences.builder()
                .user(user)
                .build();
        return notificationPreferencesRepository.save(prefs);
    }

    private NotificationPreferencesDto toPreferencesDto(NotificationPreferences prefs) {
        return new NotificationPreferencesDto(
                prefs.isEmailOnReview(),
                prefs.isEmailOnVote(),
                prefs.isEmailOnStatus(),
                prefs.isSlackEnabled(),
                prefs.getSlackWebhook());
    }

    /**
     * Returns all users in current workspace.
     */
    @Transactional(readOnly = true)
    public List<UserDto> listUsersInCurrentWorkspace() {
        return workspaceMembershipRepository.findAllByWorkspace_IdOrderByCreatedAtAsc(tenantContext.getWorkspaceId())
                .stream()
                .map(UserDto::fromMembership)
                .toList();
    }

    /**
     * Updates user role inside current workspace. ADMIN cannot change their own role.
     */
    @Transactional
    public UserDto updateRole(UUID userId, Role newRole) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (actor.getId().equals(userId)) {
            throw new BadRequestException("Cannot change your own role.");
        }

        WorkspaceMembership membership = workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(userId, tenantContext.getWorkspaceId())
                .filter(m -> m.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        User user = membership.getUser();

        String oldValue = membership.getRole().name();
        membership.setRole(newRole);
        if (user.getWorkspace().getId().equals(tenantContext.getWorkspaceId())) {
            user.setRole(newRole);
        }
        User saved = userRepository.save(user);
        workspaceMembershipRepository.save(membership);

        auditService.record(actor, actor.getWorkspace(), AuditActions.ROLE_CHANGED, "USER", saved.getId(),
                "{\"role\":\"" + oldValue + "\"}",
                "{\"role\":\"" + newRole.name() + "\"}");

        log.info("Role updated for user {} from {} to {} by {}", saved.getEmail(), oldValue, saved.getRole(), actor.getEmail());
        return UserDto.fromMembership(membership);
    }

    /**
     * Activates or deactivates a user account inside the current workspace.
     * ADMIN cannot deactivate their own account.
     */
    @Transactional
    public UserDto updateStatus(UUID userId, boolean isActive) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (actor.getId().equals(userId)) {
            throw new BadRequestException("Cannot change your own active status.");
        }

        WorkspaceMembership membership = workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(userId, tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if (membership.getStatus() == WorkspaceMembershipStatus.PENDING) {
            throw new BadRequestException("Cannot change the status of a pending invitation.");
        }

        boolean oldValue = membership.getStatus() == WorkspaceMembershipStatus.ACTIVE;
        membership.setStatus(isActive ? WorkspaceMembershipStatus.ACTIVE : WorkspaceMembershipStatus.SUSPENDED);
        workspaceMembershipRepository.save(membership);

        auditService.record(actor, actor.getWorkspace(), AuditActions.USER_STATUS_CHANGED, "WORKSPACE_MEMBERSHIP",
                membership.getId(), "{\"isActive\":" + oldValue + "}", "{\"isActive\":" + isActive + "}");

        log.info("Workspace membership status updated for user {} from {} to {} by {}",
                membership.getUser().getEmail(), oldValue, isActive, actor.getEmail());
        return UserDto.fromMembership(membership);
    }

    /**
     * Creates an inactive placeholder user and sends an invitation email.
     * The invitee clicks the link to set up their password and activate their account.
     */
    @Transactional
    public InviteUserResponse inviteUser(String email, Role role) {
        String normalizedEmail = email.trim().toLowerCase();

        Workspace workspace = workspaceRepository.findById(tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User invitedUser = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userRepository.save(User.builder()
                        .workspace(workspace)
                        .email(normalizedEmail)
                        .passwordHash(null)
                        .fullName("Pending invitation")
                        .role(role)
                        .emailVerified(false)
                        .isActive(false)
                        .totpSetupRequired(true)
                        .build()));

        workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(invitedUser.getId(), workspace.getId())
                .ifPresent(existing -> {
                    if (existing.getStatus() == WorkspaceMembershipStatus.ACTIVE) {
                        throw new ConflictException("This user is already a member of the workspace.");
                    }
                });

        WorkspaceMembership membership = workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(invitedUser.getId(), workspace.getId())
                .orElseGet(() -> WorkspaceMembership.builder()
                        .user(invitedUser)
                        .workspace(workspace)
                        .createdAt(Instant.now())
                        .build());
        membership.setRole(role);
        membership.setStatus(WorkspaceMembershipStatus.PENDING);
        membership.setAcceptedAt(null);
        workspaceMembershipRepository.save(membership);

        String token = verificationTokenService.createWorkspaceToken(invitedUser, workspace, TokenType.WORKSPACE_INVITE, 168);
        String inviteUrl = appProperties.getFrontendUrl() + "/accept-invite?token=" + token;
        mailService.sendInvitationEmail(normalizedEmail, workspace.getName(), role.name(), inviteUrl);

        auditService.record(actor, workspace, AuditActions.USER_INVITED, "USER", invitedUser.getId(), null,
                "{\"email\":\"" + normalizedEmail + "\",\"role\":\"" + role.name() + "\"}");

        log.info("Invitation sent to {} as {} by {}", normalizedEmail, role, actor.getEmail());
        VerificationToken latestToken = verificationTokenRepository.findAllWorkspaceTokens(workspace.getId(), TokenType.WORKSPACE_INVITE)
                .stream()
                .filter(v -> v.getUser().getId().equals(invitedUser.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Invitation token not found."));
        return new InviteUserResponse(
                "Invitation sent to " + normalizedEmail,
                inviteUrl,
                toInvitationDto(latestToken));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationDto> listWorkspaceInvitations() {
        return verificationTokenRepository.findAllWorkspaceTokens(tenantContext.getWorkspaceId(), TokenType.WORKSPACE_INVITE)
                .stream()
                .map(this::toInvitationDto)
                .toList();
    }

    private WorkspaceInvitationDto toInvitationDto(VerificationToken token) {
        Workspace workspace = token.getWorkspace() != null ? token.getWorkspace() : token.getUser().getWorkspace();
        String status;
        if (token.isUsed()) {
            status = "ACCEPTED";
        } else if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            status = "EXPIRED";
        } else {
            status = "PENDING";
        }
        return new WorkspaceInvitationDto(
                token.getId(),
                token.getUser().getId(),
                token.getUser().getEmail(),
                workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(token.getUser().getId(), workspace.getId())
                        .map(WorkspaceMembership::getRole)
                        .orElse(token.getUser().getRole()),
                status,
                token.getCreatedAt(),
                token.getExpiresAt());
    }
}
