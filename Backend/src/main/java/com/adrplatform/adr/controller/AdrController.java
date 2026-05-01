package com.adrplatform.adr.controller;

import com.adrplatform.adr.domain.AdrStatus;
import com.adrplatform.adr.dto.AdrDto;
import com.adrplatform.adr.dto.CreateAdrRequest;
import com.adrplatform.adr.dto.StatusTransitionRequest;
import com.adrplatform.adr.dto.UpdateAdrRequest;
import com.adrplatform.adr.service.AdrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/adrs")
@RequiredArgsConstructor
public class AdrController {

    private final AdrService adrService;

    @Operation(summary = "List all ADRs in the current workspace")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping
    public ResponseEntity<List<AdrDto>> list(
            @Parameter(description = "Filter by status") @RequestParam(value = "status", required = false) AdrStatus status,
            @Parameter(description = "Search in title/context/decision/alternatives") @RequestParam(value = "search", required = false) String search
    ) {
        return ResponseEntity.ok(adrService.getAllAdrs(status, search));
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
        return ResponseEntity.ok(adrService.transitionStatus(id, request.status()));
    }

    @Operation(summary = "Delete an ADR (ADMIN only)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        adrService.deleteAdr(id);
        return ResponseEntity.noContent().build();
    }
}
