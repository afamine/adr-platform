package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.UpdateRoleRequest;
import com.adrplatform.auth.dto.UpdateRoleResponse;
import com.adrplatform.auth.dto.UpdateStatusRequest;
import com.adrplatform.auth.dto.UpdateStatusResponse;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Operation(summary = "List users in current workspace")
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserDto>> listUsers() {
        return ResponseEntity.ok(userService.listUsersInCurrentWorkspace());
    }

    @Operation(summary = "Update a user role")
    @ApiResponse(responseCode = "200", description = "Role updated")
    @ApiResponse(responseCode = "400", description = "Invalid request or self-edit attempt")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/role")
    public ResponseEntity<UpdateRoleResponse> updateRole(@PathVariable UUID id,
                                                         @Valid @RequestBody UpdateRoleRequest request) {
        UserDto updated = userService.updateRole(id, request.getRole());
        return ResponseEntity.ok(UpdateRoleResponse.builder()
                .message("Role updated successfully.")
                .userId(updated.getId().toString())
                .newRole(updated.getRole().name())
                .build());
    }

    @Operation(summary = "Activate or deactivate a user account")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "400", description = "Invalid request or self-edit attempt")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<UpdateStatusResponse> updateStatus(@PathVariable UUID id,
                                                             @Valid @RequestBody UpdateStatusRequest request) {
        UserDto updated = userService.updateStatus(id, request.getIsActive());
        return ResponseEntity.ok(UpdateStatusResponse.builder()
                .message("User status updated.")
                .isActive(updated.isActive())
                .build());
    }
}
