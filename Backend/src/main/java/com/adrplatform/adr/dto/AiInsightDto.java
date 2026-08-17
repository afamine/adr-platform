package com.adrplatform.adr.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiInsightDto(
    java.util.UUID id,
    String title,
    String summary,
    String impact,
    int confidence,
    String rationale,
    String sourceReference,
    String sourceQuote
) {}
