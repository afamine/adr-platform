package com.adrplatform.adr.controller;

import com.adrplatform.adr.domain.AdrStatus;
import com.adrplatform.adr.dto.AdrDto;
import com.adrplatform.adr.dto.AdrSummaryDto;
import com.adrplatform.adr.dto.AiInsightDto;
import com.adrplatform.adr.dto.AuditEventDto;
import com.adrplatform.adr.dto.CreateAdrRequest;
import com.adrplatform.adr.dto.StatusTransitionRequest;
import com.adrplatform.adr.dto.UpdateAdrRequest;
import com.adrplatform.adr.service.AdrAuditService;
import com.adrplatform.adr.service.AdrService;
import com.adrplatform.adr.service.AiInsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/adrs")
@RequiredArgsConstructor
public class AdrController {

    private final AdrService adrService;
    private final AdrAuditService adrAuditService;
    private final AiInsightService aiInsightService;

    @Operation(summary = "Get the most recently updated ADRs with last-activity info")
    @ApiResponse(responseCode = "200", description = "Recent ADRs returned")
    @GetMapping("/recent")
    public ResponseEntity<List<AdrDto>> recent(
            @Parameter(description = "Maximum number of results") @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(adrService.getRecentAdrs(limit));
    }

    @Operation(summary = "List all ADRs in the current workspace")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    public ResponseEntity<List<AdrDto>> list(
            @Parameter(description = "Filter by status") @RequestParam(value = "status", required = false) AdrStatus status,
            @Parameter(description = "Search in title/context/decision/alternatives") @RequestParam(value = "search", required = false) String search,
            @Parameter(description = "Filter by exact tag name") @RequestParam(value = "tag", required = false) String tag
    ) {
        return ResponseEntity.ok(adrService.getAllAdrs(status, search, tag));
    }

    @Operation(summary = "List ADRs with pagination and sorting")
    @ApiResponse(responseCode = "200", description = "Paged list returned")
    @GetMapping("/paged")
    public ResponseEntity<Page<AdrDto>> listPaged(
            @Parameter(description = "Filter by status") @RequestParam(required = false) AdrStatus status,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by exact tag name") @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "adrNumber") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {
        String sortColumn = sort.equals("adrNumber") ? "adr_number" : sort;
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(direction), sortColumn));
        return ResponseEntity.ok(adrService.getAllAdrs(status, search, tag, pageable));
    }

    @Operation(summary = "List ADRs eligible to be used as a replacement when superseding")
    @ApiResponse(responseCode = "200", description = "Eligible ADRs returned")
    @GetMapping("/eligible-as-replacement")
    public ResponseEntity<List<AdrSummaryDto>> getEligibleReplacements() {
        return ResponseEntity.ok(adrService.getEligibleReplacements());
    }

    @Operation(summary = "Get a single ADR by id")
    @ApiResponse(responseCode = "200", description = "ADR found")
    @ApiResponse(responseCode = "404", description = "ADR not found", content = @Content)
    @GetMapping("/{id}")
    public ResponseEntity<AdrDto> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(adrService.getAdrById(id));
    }

    @Operation(summary = "Create a new ADR (status=DRAFT)")
    @ApiResponse(responseCode = "201", description = "ADR created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    @PostMapping
    @PreAuthorize("hasAnyRole('AUTHOR', 'ADMIN')")
    public ResponseEntity<AdrDto> create(@Valid @RequestBody CreateAdrRequest request) {
        AdrDto created = adrService.createAdr(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update ADR fields (not status)")
    @ApiResponse(responseCode = "200", description = "ADR updated")
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    @PutMapping("/{id}")
    public ResponseEntity<AdrDto> update(@PathVariable("id") UUID id, @Valid @RequestBody UpdateAdrRequest request) {
        return ResponseEntity.ok(adrService.updateAdr(id, request));
    }

    @Operation(summary = "Transition ADR status")
    @ApiResponse(responseCode = "200", description = "Status changed")
    @ApiResponse(responseCode = "400", description = "Invalid transition")
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdrDto> transition(@PathVariable("id") UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return ResponseEntity.ok(adrService.transitionStatus(id, request));
    }

    @Operation(summary = "Delete an ADR (ADMIN only)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        adrService.deleteAdr(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get audit log for an ADR")
    @ApiResponse(responseCode = "200", description = "Audit log returned")
    @ApiResponse(responseCode = "404", description = "ADR not found", content = @Content)
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditEventDto>> audit(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(adrAuditService.getAuditLog(id));
    }

    @Operation(summary = "Generate AI insights for an ADR")
    @ApiResponse(responseCode = "200", description = "Insights generated (may be empty if AI unavailable)")
    @GetMapping("/{id}/ai-insights")
    public ResponseEntity<List<AiInsightDto>> aiInsights(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(aiInsightService.generateInsights(adrService.getAdrEntity(id)));
    }

    @Operation(summary = "Export an ADR as a MADR-format Markdown file")
    @ApiResponse(responseCode = "200", description = "Markdown file returned")
    @ApiResponse(responseCode = "404", description = "ADR not found", content = @Content)
    @GetMapping("/{id}/export")
    public ResponseEntity<String> exportMarkdown(@PathVariable("id") UUID id) {
        AdrDto adr = adrService.getAdrById(id);
        String markdown = adrService.buildMadrMarkdown(adr);
        String filename = "ADR-" + adr.adrNumber() + "-"
                + adr.title().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".md";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(markdown);
    }
}
