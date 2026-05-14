package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.MessageResponse;
import com.adrplatform.auth.dto.UpdateWorkspaceRequest;
import com.adrplatform.auth.dto.WorkspaceDto;
import com.adrplatform.auth.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "Workspace configuration endpoints")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "Get current workspace info")
    @ApiResponse(responseCode = "200", description = "Workspace info retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping
    public ResponseEntity<WorkspaceDto> getWorkspace() {
        return ResponseEntity.ok(workspaceService.getCurrentWorkspace());
    }

    @Operation(summary = "Update workspace settings (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Workspace updated")
    @ApiResponse(responseCode = "400", description = "Validation error or slug taken")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<WorkspaceDto> updateWorkspace(@Valid @RequestBody UpdateWorkspaceRequest request) {
        return ResponseEntity.ok(workspaceService.updateWorkspace(request));
    }

    @Operation(summary = "Reset workspace to defaults (ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Workspace reset")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    public ResponseEntity<MessageResponse> resetWorkspace() {
        workspaceService.resetWorkspace();
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Workspace settings reset to defaults.")
                .build());
    }
}
