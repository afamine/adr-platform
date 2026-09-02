package com.adrplatform.auth.dto;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.WorkspaceMembership;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserDto {
    private UUID id;
    private UUID workspaceId;
    private String workspaceName;
    private String workspaceSlug;
    private String email;
    private String fullName;
    private Role role;
    private boolean emailVerified;
    @Getter(onMethod_ = {@JsonProperty("isActive")})
    private boolean isActive;
    private boolean totpSetupRequired;
    private Instant createdAt;
    private String avatarColor;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .workspaceId(user.getWorkspace().getId())
                .workspaceName(user.getWorkspace().getName())
                .workspaceSlug(user.getWorkspace().getSlug())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .emailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .totpSetupRequired(user.isTotpSetupRequired())
                .createdAt(user.getCreatedAt())
                .avatarColor(user.getAvatarColor())
                .build();
    }

    public static UserDto fromMembership(WorkspaceMembership membership) {
        User user = membership.getUser();
        return UserDto.builder()
                .id(user.getId())
                .workspaceId(membership.getWorkspace().getId())
                .workspaceName(membership.getWorkspace().getName())
                .workspaceSlug(membership.getWorkspace().getSlug())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(membership.getRole())
                .emailVerified(user.isEmailVerified())
                .isActive(user.isActive() && membership.getStatus() == com.adrplatform.auth.domain.WorkspaceMembershipStatus.ACTIVE)
                .totpSetupRequired(user.isTotpSetupRequired())
                .createdAt(user.getCreatedAt())
                .avatarColor(user.getAvatarColor())
                .build();
    }
}
