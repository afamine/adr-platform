package com.adrplatform.adr.service;

import com.adrplatform.adr.config.AiAssistProperties;
import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AiAnalysisInsight;
import com.adrplatform.adr.domain.AiAnalysisResult;
import com.adrplatform.adr.domain.AiAnalysisStatus;
import com.adrplatform.adr.dto.AiAnalysisResultDto;
import com.adrplatform.adr.dto.AiAnalysisTriggerResponse;
import com.adrplatform.adr.dto.AiInsightDto;
import com.adrplatform.adr.exception.AdrNotFoundException;
import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.adr.repository.AiAnalysisResultRepository;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.auth.service.AuditService;
import com.adrplatform.common.AuditActions;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final AdrRepository adrRepository;
    private final AiAnalysisResultRepository analysisRepository;
    private final AiAnalysisProcessor analysisProcessor;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final AiAssistProperties properties;

    @Transactional
    public AiAnalysisTriggerResponse triggerAnalysis(UUID adrId) {
        Adr adr = findAdrInActiveWorkspace(adrId);
        String versionHash = versionHash(adr);

        AiAnalysisResult reusable = analysisRepository
                .findByAdrVersionAndStatusWithInsights(adrId, versionHash, AiAnalysisStatus.COMPLETED)
                .stream().findFirst().orElse(null);
        if (reusable != null) {
            return new AiAnalysisTriggerResponse(reusable.getId(), reusable.getStatus());
        }

        AiAnalysisResult inProgress = analysisRepository
                .findByAdrVersionAndStatusWithInsights(adrId, versionHash, AiAnalysisStatus.IN_PROGRESS)
                .stream().findFirst().orElse(null);
        if (inProgress != null) {
            return new AiAnalysisTriggerResponse(inProgress.getId(), inProgress.getStatus());
        }

        AiAnalysisResult analysis = analysisRepository.save(AiAnalysisResult.builder()
                .adr(adr)
                .adrVersionHash(versionHash)
                .status(AiAnalysisStatus.IN_PROGRESS)
                .build());

        User actor = currentUser();
        auditService.record(actor, actor.getWorkspace(), AuditActions.AI_ANALYSIS_TRIGGERED, "ADR", adr.getId(), null,
                toJson(Map.of("analysisId", analysis.getId().toString(), "adrVersion", versionHash,
                        "provider", properties.providerNameOrDefault(), "externalDataSent", true)));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                analysisProcessor.generateAsync(analysis.getId());
            }
        });
        return new AiAnalysisTriggerResponse(analysis.getId(), analysis.getStatus());
    }

    @Transactional(readOnly = true)
    public AiAnalysisResultDto getLatestAnalysis(UUID adrId) {
        Adr adr = findAdrInActiveWorkspace(adrId);
        String currentVersionHash = versionHash(adr);
        AiAnalysisResult latest = analysisRepository.findLatestWithInsights(adrId).stream().findFirst().orElse(null);
        if (latest == null) {
            return new AiAnalysisResultDto(null, AiAnalysisStatus.STALE, null, currentVersionHash, null,
                    properties.privacyNoticeOrDefault(), List.of());
        }
        return toDto(latest, currentVersionHash);
    }

    private AiAnalysisResultDto toDto(AiAnalysisResult analysis, String currentVersionHash) {
        AiAnalysisStatus exposedStatus = analysis.getAdrVersionHash().equals(currentVersionHash)
                ? analysis.getStatus() : AiAnalysisStatus.STALE;
        List<AiInsightDto> insights = analysis.getInsights().stream().map(this::toInsightDto).toList();
        return new AiAnalysisResultDto(analysis.getId(), exposedStatus, analysis.getGeneratedAt(),
                analysis.getAdrVersionHash(), analysis.getErrorMessage(), properties.privacyNoticeOrDefault(), insights);
    }

    private AiInsightDto toInsightDto(AiAnalysisInsight insight) {
        return new AiInsightDto(insight.getId(), insight.getTitle(), insight.getSummary(),
                insight.getImpact().name(), insight.getConfidence(), insight.getRationale(),
                insight.getSourceReference(), insight.getSourceQuote());
    }

    private Adr findAdrInActiveWorkspace(UUID adrId) {
        return adrRepository.findByIdAndWorkspace_Id(adrId, tenantContext.getWorkspaceId())
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));
    }

    private String versionHash(Adr adr) {
        String content = String.join("\\u0000", safe(adr.getTitle()), safe(adr.getContext()), safe(adr.getDecision()),
                safe(adr.getConsequences()), safe(adr.getAlternatives()), safe(adr.getTagsCsv()));
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : bytes) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String safe(String value) { return value == null ? "" : value.trim(); }
    private User currentUser() { return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
    private String toJson(Map<String, Object> payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (Exception exception) { throw new IllegalStateException("Failed to serialize AI audit payload", exception); }
    }
}
