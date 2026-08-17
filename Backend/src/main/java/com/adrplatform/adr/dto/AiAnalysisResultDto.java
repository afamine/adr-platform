package com.adrplatform.adr.dto;

import com.adrplatform.adr.domain.AiAnalysisStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAnalysisResultDto(
        UUID analysisId,
        AiAnalysisStatus status,
        Instant generatedAt,
        String adrVersion,
        String errorMessage,
        String privacyNotice,
        List<AiInsightDto> insights
) {}
