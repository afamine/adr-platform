package com.adrplatform.auth.dto;

import com.adrplatform.auth.domain.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {

    @NotNull
    private Role role;
}
