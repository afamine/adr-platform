package com.adrplatform.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateRoleResponse {
    private String message;
    private String userId;
    private String newRole;
}
