package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.MessageResponse;
import com.adrplatform.auth.dto.UpdateWorkspaceRequest;
import com.adrplatform.auth.dto.AuthResponse;
import com.adrplatform.auth.dto.WorkspaceDto;
import com.adrplatform.auth.dto.WorkspaceMembershipDto;
import com.adrplatform.auth.dto.WorkspaceSlugStatus;
import com.adrplatform.auth.service.WorkspaceService;
import com.adrplatform.auth.security.AuthCookieService;
import com.adrplatform.auth.security.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspace")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "Workspace configuration endpoints")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final AuthCookieService authCookieService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "Check whether a workspace slug can be joined")
    @ApiResponse(responseCode = "200", description = "Slug status retrieved")
    @GetMapping("/slug-status")
    public ResponseEntity<WorkspaceSlugStatus> getSlugStatus(@RequestParam("slug") String slug) {
        return ResponseEntity.ok(workspaceService.getSlugStatus(slug));
    }

    @Operation(summary = "Get current workspace info")
    @ApiResponse(responseCode = "200", description = "Workspace info retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping
    public ResponseEntity<WorkspaceDto> getWorkspace() {
        return ResponseEntity.ok(workspaceService.getCurrentWorkspace());
    }

    @Operation(summary = "List workspaces available to the current account")
    @ApiResponse(responseCode = "200", description = "Workspace memberships retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/memberships")
    public ResponseEntity<List<WorkspaceMembershipDto>> listMyWorkspaces() {
        return ResponseEntity.ok(workspaceService.listMyWorkspaces());
    }

    @Operation(summary = "Switch the current account to another workspace")
    @ApiResponse(responseCode = "200", description = "Workspace switched")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PostMapping("/switch/{workspaceId}")
    public ResponseEntity<AuthResponse> switchWorkspace(@PathVariable UUID workspaceId,
                                                        HttpServletResponse response) {
        return ResponseEntity.ok(authCookieService.writeSession(response, workspaceService.switchWorkspace(workspaceId),
                jwtProperties.getRefreshTokenTtlMs()));
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
