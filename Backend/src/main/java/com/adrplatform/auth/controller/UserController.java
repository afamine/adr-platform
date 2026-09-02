package com.adrplatform.auth.controller;

import com.adrplatform.auth.dto.InviteUserRequest;
import com.adrplatform.auth.dto.InviteUserResponse;
import com.adrplatform.auth.dto.NotificationPreferencesDto;
import com.adrplatform.auth.dto.UpdatePreferencesRequest;
import com.adrplatform.auth.dto.UpdateProfileRequest;
import com.adrplatform.auth.dto.EmailChangeRequest;
import com.adrplatform.auth.dto.MessageResponse;
import com.adrplatform.auth.dto.UpdateRoleRequest;
import com.adrplatform.auth.dto.UpdateRoleResponse;
import com.adrplatform.auth.dto.UpdateStatusRequest;
import com.adrplatform.auth.dto.UpdateStatusResponse;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.dto.WorkspaceInvitationDto;
import com.adrplatform.auth.security.RedisRateLimiter;
import com.adrplatform.auth.security.RequestIpResolver;
import com.adrplatform.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RedisRateLimiter rateLimiter;
    private final RequestIpResolver requestIpResolver;

    @Operation(summary = "Get current user profile")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @Operation(summary = "Update current user display name")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }


    @Operation(summary = "Request a verified email-address change for the current account")
    @ApiResponse(responseCode = "200", description = "Confirmation link dispatched")
    @ApiResponse(responseCode = "400", description = "Invalid credentials or request")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @PostMapping("/me/email-change")
    public ResponseEntity<MessageResponse> requestEmailChange(@Valid @RequestBody EmailChangeRequest request,
                                                               HttpServletRequest servletRequest) {
        rateLimiter.check("email-change", requestIpResolver.resolve(servletRequest), request.newEmail(), 5, 3, Duration.ofMinutes(10));
        return ResponseEntity.ok(userService.requestEmailChange(request));
    }

    @Operation(summary = "Get notification preferences for current user")
    @ApiResponse(responseCode = "200", description = "Preferences retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/me/preferences")
    public ResponseEntity<NotificationPreferencesDto> getMyPreferences() {
        return ResponseEntity.ok(userService.getMyPreferences());
    }

    @Operation(summary = "Update notification preferences for current user")
    @ApiResponse(responseCode = "200", description = "Preferences updated")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("/me/preferences")
    public ResponseEntity<NotificationPreferencesDto> updateMyPreferences(@RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(userService.updateMyPreferences(request));
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
                .userId(updated.getId().toString())
                .isActive(updated.isActive())
                .build());
    }

    @Operation(summary = "Invite a new user to the workspace")
    @ApiResponse(responseCode = "200", description = "Invitation sent")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/invite")
    public ResponseEntity<InviteUserResponse> inviteUser(@Valid @RequestBody InviteUserRequest request) {
        return ResponseEntity.ok(userService.inviteUser(request.getEmail(), request.getRole()));
    }

    @Operation(summary = "List workspace invitations")
    @ApiResponse(responseCode = "200", description = "Invitations retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "Forbidden")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/invitations")
    public ResponseEntity<List<WorkspaceInvitationDto>> listInvitations() {
        return ResponseEntity.ok(userService.listWorkspaceInvitations());
    }
}
