package com.adrplatform.auth.service;

import com.adrplatform.auth.config.AppProperties;
import com.adrplatform.auth.domain.NotificationPreferences;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.TokenType;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.dto.NotificationPreferencesDto;
import com.adrplatform.auth.dto.UpdatePreferencesRequest;
import com.adrplatform.auth.dto.UpdateProfileRequest;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.exception.ConflictException;
import com.adrplatform.common.AuditActions;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.repository.NotificationPreferencesRepository;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.TenantContext;
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
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final VerificationTokenService verificationTokenService;
    private final MailService mailService;
    private final AppProperties appProperties;

    /**
     * Returns current authenticated user profile.
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        User principal = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByIdAndWorkspace_Id(principal.getId(), tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.fromEntity(user);
    }

    /**
     * Updates the display name of the currently authenticated user.
     */
    @Transactional
    public UserDto updateMyProfile(UpdateProfileRequest request) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByIdAndWorkspace_Id(actor.getId(), tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

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
        return UserDto.fromEntity(saved);
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
        return userRepository.findAllByWorkspace_Id(tenantContext.getWorkspaceId())
                .stream()
                .map(UserDto::fromEntity)
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

        User user = userRepository.findByIdAndWorkspace_Id(userId, tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String oldValue = user.getRole().name();
        user.setRole(newRole);
        User saved = userRepository.save(user);

        auditService.record(actor, actor.getWorkspace(), AuditActions.ROLE_CHANGED, "USER", saved.getId(),
                "{\"role\":\"" + oldValue + "\"}",
                "{\"role\":\"" + saved.getRole().name() + "\"}");

        log.info("Role updated for user {} from {} to {} by {}", saved.getEmail(), oldValue, saved.getRole(), actor.getEmail());
        return UserDto.fromEntity(saved);
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

        User user = userRepository.findByIdAndWorkspace_Id(userId, tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        boolean oldValue = user.isActive();
        user.setActive(isActive);
        User saved = userRepository.save(user);

        auditService.record(actor, actor.getWorkspace(), AuditActions.USER_STATUS_CHANGED, "USER", saved.getId(),
                "{\"isActive\":" + oldValue + "}",
                "{\"isActive\":" + saved.isActive() + "}");

        log.info("Status updated for user {} from {} to {} by {}", saved.getEmail(), oldValue, saved.isActive(), actor.getEmail());
        return UserDto.fromEntity(saved);
    }

    /**
     * Creates an inactive placeholder user and sends an invitation email.
     * The invitee clicks the link to set up their password and activate their account.
     */
    @Transactional
    public void inviteUser(String email, Role role) {
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ConflictException("A user with this email already exists in the workspace.");
        }

        Workspace workspace = workspaceRepository.findById(tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));

        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        User invitedUser = User.builder()
                .workspace(workspace)
                .email(normalizedEmail)
                .passwordHash(null)
                .fullName("Pending invitation")
                .role(role)
                .emailVerified(false)
                .isActive(false)
                .build();

        User saved = userRepository.save(invitedUser);

        String token = verificationTokenService.createToken(saved, TokenType.WORKSPACE_INVITE, 168);
        String inviteUrl = appProperties.getFrontendUrl() + "/accept-invite?token=" + token;
        mailService.sendInvitationEmail(normalizedEmail, workspace.getName(), role.name(), inviteUrl);

        auditService.record(actor, workspace, AuditActions.USER_INVITED, "USER", saved.getId(), null,
                "{\"email\":\"" + normalizedEmail + "\",\"role\":\"" + role.name() + "\"}");

        log.info("Invitation sent to {} as {} by {}", normalizedEmail, role, actor.getEmail());
    }
}
