package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.repository.UserRepository;
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
    private final TenantContext tenantContext;
    private final AuditService auditService;

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

        auditService.record(actor, actor.getWorkspace(), "ROLE_CHANGED", "USER", saved.getId(),
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

        auditService.record(actor, actor.getWorkspace(), "STATUS_CHANGED", "USER", saved.getId(),
                "{\"isActive\":" + oldValue + "}",
                "{\"isActive\":" + saved.isActive() + "}");

        log.info("Status updated for user {} from {} to {} by {}", saved.getEmail(), oldValue, saved.isActive(), actor.getEmail());
        return UserDto.fromEntity(saved);
    }
}
