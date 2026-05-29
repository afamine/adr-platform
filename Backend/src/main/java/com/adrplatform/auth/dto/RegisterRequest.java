package com.adrplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    @Size(min = 3, max = 50, message = "Workspace name must be between 3 and 50 characters")
    private String workspaceName;

    @Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "Workspace slug must be lowercase letters, digits and hyphens only (e.g. my-workspace)"
    )
    @Size(min = 3, max = 60, message = "Workspace slug must be between 3 and 60 characters")
    private String workspaceSlug;
}
