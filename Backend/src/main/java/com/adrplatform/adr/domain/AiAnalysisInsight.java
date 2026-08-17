package com.adrplatform.adr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity @Table(name = "ai_analysis_insight")
public class AiAnalysisInsight {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private AiAnalysisResult analysis;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String summary;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private AiInsightImpact impact;
    @Column(nullable = false) private int confidence;
    @Column(nullable = false, columnDefinition = "TEXT") private String rationale;
    @Column(name = "source_reference", nullable = false, length = 32) private String sourceReference;
    @Column(name = "source_quote", columnDefinition = "TEXT") private String sourceQuote;
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); }
}
