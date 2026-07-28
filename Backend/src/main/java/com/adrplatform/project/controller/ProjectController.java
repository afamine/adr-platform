package com.adrplatform.project.controller;

import com.adrplatform.project.dto.CreateProjectRequest;
import com.adrplatform.project.dto.ProjectDto;
import com.adrplatform.project.dto.UpdateProjectRequest;
import com.adrplatform.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDto>> list() { return ResponseEntity.ok(projectService.listProjects()); }

    @PostMapping
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ProjectDto> archive(@PathVariable UUID id) { return ResponseEntity.ok(projectService.archiveProject(id)); }
}
