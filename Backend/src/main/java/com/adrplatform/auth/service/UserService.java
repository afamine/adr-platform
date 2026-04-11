package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.dto.UserDto;
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
     * Updates user role inside current workspace.
     */
    @Transactional
    public UserDto updateRole(UUID userId, Role newRole) {
        User user = userRepository.findByIdAndWorkspace_Id(userId, tenantContext.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found in workspace"));

        String oldValue = user.getRole().name();
        user.setRole(newRole);
        User saved = userRepository.save(user);

        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        auditService.record(actor, actor.getWorkspace(), "ROLE_CHANGED", "USER", saved.getId(),
                "{\"role\":\"" + oldValue + "\"}",
                "{\"role\":\"" + saved.getRole().name() + "\"}");

        log.info("Role updated for user {} from {} to {}", saved.getEmail(), oldValue, saved.getRole());
        return UserDto.fromEntity(saved);
    }
}
