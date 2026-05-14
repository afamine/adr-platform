package com.adrplatform.auth.dto;

import com.adrplatform.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteUserRequest {

    @NotBlank
    @Email
    private String email;

    @NotNull
    private Role role;
}
