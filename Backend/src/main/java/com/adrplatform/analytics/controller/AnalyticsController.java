package com.adrplatform.analytics.controller;

import com.adrplatform.analytics.dto.KpiDto;
import com.adrplatform.analytics.dto.StatusCountDto;
import com.adrplatform.analytics.dto.WeeklyActivityDto;
import com.adrplatform.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','APPROVER')")
@Tag(name = "Analytics", description = "Dashboard analytics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get KPI summary for the current workspace")
    @ApiResponse(responseCode = "200", description = "KPIs returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @GetMapping("/kpis")
    public ResponseEntity<KpiDto> getKpis() {
        return ResponseEntity.ok(analyticsService.getKpis());
    }

    @Operation(summary = "Get ADR count grouped by status")
    @ApiResponse(responseCode = "200", description = "Distribution returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @GetMapping("/status-distribution")
    public ResponseEntity<List<StatusCountDto>> getStatusDistribution() {
        return ResponseEntity.ok(analyticsService.getStatusDistribution());
    }

    @Operation(summary = "Get weekly ADR creation activity for the last N weeks")
    @ApiResponse(responseCode = "200", description = "Weekly activity returned")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @GetMapping("/weekly-activity")
    public ResponseEntity<List<WeeklyActivityDto>> getWeeklyActivity(
            @RequestParam(defaultValue = "5") int weeks) {
        return ResponseEntity.ok(analyticsService.getWeeklyActivity(weeks));
    }
}
