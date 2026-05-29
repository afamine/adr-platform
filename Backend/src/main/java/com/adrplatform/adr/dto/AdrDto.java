package com.adrplatform.adr.dto;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AdrStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record AdrDto(
        UUID id,
        Integer adrNumber,
        String title,
        AdrStatus status,
        String context,
        String decision,
        String consequences,
        String alternatives,
        List<String> tags,
        UUID authorId,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID workspaceId,
        Instant reviewStartedAt,
        String lastAction,
        LocalDateTime lastActionDate,
        UUID supersededById,
        Integer supersededByAdrNumber,
        String supersededByTitle,
        UUID supersedesId,
        Integer supersedesAdrNumber,
        String supersedesTitle
) {
    private static List<String> parseTags(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        String stripped = csv.startsWith(",") ? csv.substring(1) : csv;
        if (stripped.endsWith(",")) stripped = stripped.substring(0, stripped.length() - 1);
        return Arrays.stream(stripped.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static AdrDto fromEntity(Adr a) {
        List<String> tagsList = parseTags(a.getTagsCsv());
        return new AdrDto(
                a.getId(),
                a.getAdrNumber(),
                a.getTitle(),
                a.getStatus(),
                a.getContext(),
                a.getDecision(),
                a.getConsequences(),
                a.getAlternatives(),
                tagsList,
                a.getAuthor().getId(),
                a.getAuthor().getFullName(),
                LocalDateTime.ofInstant(a.getCreatedAt(), ZoneOffset.UTC),
                LocalDateTime.ofInstant(a.getUpdatedAt(), ZoneOffset.UTC),
                a.getWorkspace().getId(),
                a.getReviewStartedAt(),
                null,
                null,
                a.getSupersededBy() != null ? a.getSupersededBy().getId() : null,
                a.getSupersededBy() != null ? a.getSupersededBy().getAdrNumber() : null,
                a.getSupersededBy() != null ? a.getSupersededBy().getTitle() : null,
                a.getSupersedes() != null ? a.getSupersedes().getId() : null,
                a.getSupersedes() != null ? a.getSupersedes().getAdrNumber() : null,
                a.getSupersedes() != null ? a.getSupersedes().getTitle() : null
        );
    }

    public static AdrDto fromEntityWithActivity(Adr a, String lastAction, LocalDateTime lastActionDate) {
        List<String> tagsList = parseTags(a.getTagsCsv());
        return new AdrDto(
                a.getId(),
                a.getAdrNumber(),
                a.getTitle(),
                a.getStatus(),
                a.getContext(),
                a.getDecision(),
                a.getConsequences(),
                a.getAlternatives(),
                tagsList,
                a.getAuthor().getId(),
                a.getAuthor().getFullName(),
                LocalDateTime.ofInstant(a.getCreatedAt(), ZoneOffset.UTC),
                LocalDateTime.ofInstant(a.getUpdatedAt(), ZoneOffset.UTC),
                a.getWorkspace().getId(),
                a.getReviewStartedAt(),
                lastAction,
                lastActionDate,
                a.getSupersededBy() != null ? a.getSupersededBy().getId() : null,
                a.getSupersededBy() != null ? a.getSupersededBy().getAdrNumber() : null,
                a.getSupersededBy() != null ? a.getSupersededBy().getTitle() : null,
                a.getSupersedes() != null ? a.getSupersedes().getId() : null,
                a.getSupersedes() != null ? a.getSupersedes().getAdrNumber() : null,
                a.getSupersedes() != null ? a.getSupersedes().getTitle() : null
        );
    }
}
