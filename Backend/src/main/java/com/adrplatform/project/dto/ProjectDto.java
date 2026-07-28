package com.adrplatform.project.dto;

import com.adrplatform.project.domain.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectDto(UUID id, String name, String description, boolean archived, Instant createdAt, Instant updatedAt) {
    public static ProjectDto fromEntity(Project project) {
        return new ProjectDto(project.getId(), project.getName(), project.getDescription(), project.isArchived(), project.getCreatedAt(), project.getUpdatedAt());
    }
}
