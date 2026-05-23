package com.adrplatform.adr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiInsightDto(
    String title,
    double confidence,
    String impact,
    String description,
    String rationale,
    String source
) {}
