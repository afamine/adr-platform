package com.adrplatform.auth.dto;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
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
                .createdAt(user.getCreatedAt())
                .avatarColor(user.getAvatarColor())
                .build();
    }
}
