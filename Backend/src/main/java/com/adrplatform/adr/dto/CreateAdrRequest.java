package com.adrplatform.adr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateAdrRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String context,
        @Size(max = 5000) String decision,
        @Size(max = 5000) String consequences,
        @Size(max = 5000) String alternatives,
        @Size(max = 20) List<
                @NotBlank
                @Size(max = 40)
                @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9 _-]*$") String> tags
) {}
