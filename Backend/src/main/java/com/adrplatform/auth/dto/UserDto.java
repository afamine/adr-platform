package com.adrplatform.auth.dto;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserDto {
    private UUID id;
    private UUID workspaceId;
    private String email;
    private String fullName;
    private Role role;
    private Instant createdAt;

    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .workspaceId(user.getWorkspace().getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
