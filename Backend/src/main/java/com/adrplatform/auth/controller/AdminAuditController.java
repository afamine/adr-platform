package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.AdminAuditEventDto;
import com.adrplatform.auth.dto.UserSummaryDto;
import com.adrplatform.auth.service.AdminAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Audit", description = "Workspace-level audit log endpoints (ADMIN only)")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    @Operation(summary = "Get paginated workspace-level audit log")
    @ApiResponse(responseCode = "200", description = "Audit events returned")
    @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    @GetMapping
    public ResponseEntity<Page<AdminAuditEventDto>> getAuditLog(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                adminAuditService.getWorkspaceAudit(actorId, action, from, to, pageable));
    }

    @Operation(summary = "Get distinct actors (users) who have audit events in this workspace")
    @ApiResponse(responseCode = "200", description = "Actor list returned")
    @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    @GetMapping("/actors")
    public ResponseEntity<List<UserSummaryDto>> getActors() {
        return ResponseEntity.ok(adminAuditService.getWorkspaceActors());
    }

    @Operation(summary = "Export workspace audit log as CSV (max 5000 rows)")
    @ApiResponse(responseCode = "200", description = "CSV file returned")
    @ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required")
    @GetMapping("/export")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        String csv = adminAuditService.exportCsv(actorId, action, from, to);
        String filename = "audit-log-" + Instant.now().toString().substring(0, 10) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
