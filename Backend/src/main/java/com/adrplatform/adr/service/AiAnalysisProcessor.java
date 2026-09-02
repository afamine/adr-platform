package com.adrplatform.adr.service;

import com.adrplatform.adr.domain.AiAnalysisInsight;
import com.adrplatform.adr.domain.AiAnalysisResult;
import com.adrplatform.adr.domain.AiAnalysisStatus;
import com.adrplatform.adr.domain.AiInsightImpact;
import com.adrplatform.adr.repository.AiAnalysisResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisProcessor {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    private final AiAnalysisResultRepository analysisRepository;

    @Async
    @Transactional
    public void generateAsync(UUID analysisId) {
        AiAnalysisResult analysis = analysisRepository.findWithInsightsById(analysisId)
                .orElseThrow(() -> new IllegalArgumentException("AI analysis not found"));

        try {
            String raw = chatClientBuilder.build().prompt()
                    .user(buildPrompt(analysis))
                    .call()
                    .content();
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("AI provider returned an empty completion. "
                        + "The configured model may have exhausted its completion-token budget before producing content.");
            }
            List<GeneratedInsight> generated = objectMapper.readValue(
                    raw.replaceAll("(?s)```json\\s*|```\\s*", "").trim(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, GeneratedInsight.class)
            );

            if (generated.isEmpty()) {
                throw new IllegalStateException("The AI provider returned no insights");
            }

            analysis.replaceInsights(generated.stream().limit(4).map(this::toEntity).toList());
            analysis.setStatus(AiAnalysisStatus.COMPLETED);
            analysis.setGeneratedAt(Instant.now());
            analysis.setErrorMessage(null);
            analysisRepository.save(analysis);
        } catch (Exception exception) {
            Throwable root = exception.getCause() != null ? exception.getCause() : exception;
            log.error("AI analysis failed for {}: {}", analysisId, root.getMessage());
            analysis.setStatus(AiAnalysisStatus.FAILED);
            analysis.setGeneratedAt(Instant.now());
            analysis.setErrorMessage("AI service temporarily unavailable. Please try again.");
            analysisRepository.save(analysis);
        }
    }

    private String buildPrompt(AiAnalysisResult analysis) {
        var adr = analysis.getAdr();
        return """
                You are a senior software architect reviewing an Architecture Decision Record (ADR).
                Analyse the ADR below and return exactly 3 to 4 useful architectural insights.

                ADR TITLE: %s
                CONTEXT: %s
                DECISION: %s
                CONSEQUENCES: %s
                ALTERNATIVES CONSIDERED: %s
                TAGS: %s

                Respond only with a valid JSON array. Do not return markdown or any text outside the array.
                Each element must match this schema exactly:
                {
                  "title": "short headline, at most 8 words",
                  "summary": "one concise sentence",
                  "impact": "HIGH|MEDIUM|LOW",
                  "confidence": 0,
                  "rationale": "one or two sentences explaining why this matters",
                  "sourceReference": "context|decision|consequences|alternatives",
                  "sourceQuote": "a short exact sentence or excerpt from the cited ADR section"
                }
                Confidence must be an integer between 0 and 100. Do not invent a source quote.
                """.formatted(
                safe(adr.getTitle()), safe(adr.getContext()), safe(adr.getDecision()),
                safe(adr.getConsequences()), safe(adr.getAlternatives()), safe(adr.getTagsCsv())
        );
    }

    private AiAnalysisInsight toEntity(GeneratedInsight insight) {
        return AiAnalysisInsight.builder()
                .title(nonBlank(insight.title(), "Untitled insight"))
                .summary(nonBlank(insight.summary(), "No summary returned."))
                .impact(parseImpact(insight.impact()))
                .confidence(Math.max(0, Math.min(100, insight.confidence())))
                .rationale(nonBlank(insight.rationale(), "No rationale returned."))
                .sourceReference(parseSourceReference(insight.sourceReference()))
                .sourceQuote(insight.sourceQuote() == null ? null : insight.sourceQuote().trim())
                .build();
    }

    private AiInsightImpact parseImpact(String value) {
        try { return AiInsightImpact.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (Exception ignored) { return AiInsightImpact.MEDIUM; }
    }

    private String parseSourceReference(String value) {
        if (value == null) return "decision";
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "context", "decision", "consequences", "alternatives" -> normalized;
            default -> "decision";
        };
    }

    private String safe(String value) { return value == null || value.isBlank() ? "Not provided" : value; }
    private String nonBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    private record GeneratedInsight(String title, String summary, String impact, int confidence,
                                    String rationale, String sourceReference, String sourceQuote) {}
}
