package com.adrplatform.adr.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAdrRequest(
        @Size(max = 255) String title,
        String context,
        String decision,
        String consequences,
        String alternatives,
        List<String> tags
) {}
