package com.adrplatform.adr.dto;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AdrStatus;

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
        UUID workspaceId
) {
    public static AdrDto fromEntity(Adr a) {
        List<String> tagsList = a.getTagsCsv() == null || a.getTagsCsv().isBlank()
                ? List.of()
                : Arrays.stream(a.getTagsCsv().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
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
                a.getWorkspace().getId()
        );
    }
}
