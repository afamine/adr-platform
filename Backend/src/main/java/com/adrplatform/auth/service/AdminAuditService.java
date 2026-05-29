package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.AuditEvent;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.dto.AdminAuditEventDto;
import com.adrplatform.auth.dto.UserSummaryDto;
import com.adrplatform.auth.repository.AuditEventRepository;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuditService {

    private final AuditEventRepository auditEventRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;

    private static final Map<String, String> ACTION_LABELS = Map.ofEntries(
            Map.entry("ADR_CREATED",         "ADR Created"),
            Map.entry("ADR_UPDATED",         "ADR Updated"),
            Map.entry("ADR_STATUS_CHANGED",  "Status Changed"),
            Map.entry("STATUS_CHANGED",      "Status Changed"),
            Map.entry("ADR_DELETED",         "ADR Deleted"),
            Map.entry("VOTE_CAST",           "Vote Cast"),
            Map.entry("COMMENT_ADDED",       "Comment Added"),
            Map.entry("USER_LOGGED_IN",      "User Logged In"),
            Map.entry("USER_LOGGED_OUT",     "User Logged Out"),
            Map.entry("USER_REGISTERED",     "User Registered"),
            Map.entry("USER_INVITE_ACCEPTED","Invite Accepted"),
            Map.entry("USER_INVITED",        "User Invited"),
            Map.entry("ROLE_CHANGED",        "User Role Changed"),
            Map.entry("USER_STATUS_CHANGED", "User Status Changed"),
            Map.entry("PASSWORD_CHANGED",    "Password Changed"),
            Map.entry("PROFILE_UPDATED",     "Profile Updated"),
            Map.entry("WORKSPACE_UPDATED",   "Workspace Updated"),
            Map.entry("WORKSPACE_RESET",     "Workspace Reset"),
            Map.entry("LOGOUT_ALL_DEVICES",  "Logout All Devices"),
            Map.entry("TOKEN_REFRESHED",     "Token Refreshed"),
            Map.entry("USER_EMAIL_VERIFIED", "Email Verified")
    );

    public Page<AdminAuditEventDto> getWorkspaceAudit(
            UUID actorId,
            String action,
            Instant from,
            Instant to,
            Pageable pageable) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        Page<AuditEvent> events = auditEventRepository.findWorkspaceAuditEvents(
                workspaceId, actorId, action, from, to, pageable);
        return events.map(this::toAdminDto);
    }

    public List<UserSummaryDto> getWorkspaceActors() {
        UUID workspaceId = tenantContext.getWorkspaceId();
        return auditEventRepository.findActorsInWorkspace(workspaceId)
                .stream()
                .map(u -> new UserSummaryDto(u.getId(), u.getFullName(), u.getEmail()))
                .toList();
    }

    public String exportCsv(UUID actorId, String action, Instant from, Instant to) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        Pageable all = PageRequest.of(0, 5000, Sort.by("createdAt").descending());
        Page<AuditEvent> events = auditEventRepository.findWorkspaceAuditEvents(
                workspaceId, actorId, action, from, to, all);

        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Actor,Email,Action,Entity,Detail\n");
        for (AuditEvent e : events.getContent()) {
            csv.append(String.join(",",
                    quoteCsv(e.getCreatedAt() != null ? e.getCreatedAt().toString() : ""),
                    quoteCsv(e.getUser() != null ? e.getUser().getFullName() : "System"),
                    quoteCsv(e.getUser() != null ? e.getUser().getEmail() : ""),
                    quoteCsv(e.getAction()),
                    quoteCsv(e.getEntityType() + ":" + e.getEntityId()),
                    quoteCsv(buildDetail(e))
            )).append("\n");
        }
        return csv.toString();
    }

    private AdminAuditEventDto toAdminDto(AuditEvent event) {
        User actor = event.getUser();
        String actionLabel = ACTION_LABELS.getOrDefault(event.getAction(), event.getAction());
        return new AdminAuditEventDto(
                event.getId(),
                event.getCreatedAt() != null ? event.getCreatedAt().toString() : null,
                actor != null ? actor.getId().toString() : null,
                actor != null ? actor.getFullName() : "System",
                actor != null ? actor.getEmail() : null,
                actor != null ? buildInitials(actor.getFullName()) : "SY",
                event.getAction(),
                actionLabel,
                event.getEntityType(),
                event.getEntityId() != null ? event.getEntityId().toString() : null,
                buildEntityLabel(event),
                buildDetail(event),
                event.getOldValueJson(),
                event.getNewValueJson(),
                null
        );
    }

    private String buildEntityLabel(AuditEvent event) {
        return event.getEntityType() + ":" + event.getEntityId();
    }

    private String buildDetail(AuditEvent event) {
        if (event.getNewValueJson() == null || event.getNewValueJson().isBlank()) return "";
        try {
            if (event.getAction() != null && event.getAction().contains("STATUS")) {
                String oldStatus = extractJsonField(event.getOldValueJson(), "status");
                String newStatus = extractJsonField(event.getNewValueJson(), "status");
                if (oldStatus != null && newStatus != null) {
                    return oldStatus + " \u2192 " + newStatus;
                }
                if (newStatus != null) {
                    return "Status: " + newStatus;
                }
            }
            return "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractJsonField(String json, String key) {
        if (json == null || json.isBlank()) return null;
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "??";
        return Arrays.stream(fullName.split("\\s+"))
                .filter(s -> !s.isBlank())
                .map(s -> String.valueOf(s.charAt(0)).toUpperCase())
                .limit(2)
                .collect(Collectors.joining());
    }

    private String quoteCsv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
