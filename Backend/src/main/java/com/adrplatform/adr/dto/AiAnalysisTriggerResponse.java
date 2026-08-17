package com.adrplatform.adr.dto;

import com.adrplatform.adr.domain.AiAnalysisStatus;
import java.util.UUID;

public record AiAnalysisTriggerResponse(UUID analysisId, AiAnalysisStatus status) {}
