package com.adrplatform.adr.service;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.dto.AiInsightDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    private ChatClient chatClient;

    @PostConstruct
    public void init() {
        this.chatClient = chatClientBuilder.build();
    }

    @Cacheable(value = "ai-insights", key = "#adr.id", unless = "#result.isEmpty()")
    public List<AiInsightDto> generateInsights(Adr adr) {
        try {
            String prompt = """
                    You are a senior software architect reviewing an Architecture Decision Record (ADR).
                    Analyse the ADR below and return EXACTLY 3 to 4 architectural insights.

                    ADR TITLE: %s
                    CONTEXT: %s
                    DECISION: %s
                    CONSEQUENCES: %s
                    ALTERNATIVES CONSIDERED: %s
                    TAGS: %s

                    Respond ONLY with a valid JSON array — no markdown, no preamble, no explanation outside the array.
                    Each element must match:
                    {
                      "title":       "<short headline, max 8 words>",
                      "confidence":  <float 0.0-1.0>,
                      "impact":      "<high | medium | low>",
                      "description": "<1–2 sentences>",
                      "rationale":   "<1–2 sentences on why this matters architecturally>",
                      "source":      "<which ADR section triggered this: context|decision|consequences|alternatives>"
                    }
                    """.formatted(
                    safe(adr.getTitle()),
                    safe(adr.getContext()),
                    safe(adr.getDecision()),
                    safe(adr.getConsequences()),
                    safe(adr.getAlternatives()),
                    safe(adr.getTagsCsv())
            );

            String raw = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            String json = raw.replaceAll("(?s)```json\\s*|```\\s*", "").trim();

            List<AiInsightDto> insights = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AiInsightDto.class)
            );

            return insights;
        } catch (Exception e) {
            Throwable root = e.getCause() != null ? e.getCause() : e;
            log.error("AI insight generation failed for ADR {} [{}]: {}", adr.getId(), root.getClass().getSimpleName(), root.getMessage());
            return List.of();
        }
    }

    private String safe(String s) {
        return s != null ? s : "Not provided";
    }
}
