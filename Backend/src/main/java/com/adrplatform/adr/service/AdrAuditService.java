package com.adrplatform.adr.service;

import com.adrplatform.adr.dto.AuditEventDto;
import com.adrplatform.adr.exception.AdrNotFoundException;
import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.auth.domain.AuditEvent;
import com.adrplatform.auth.repository.AuditEventRepository;
import com.adrplatform.auth.security.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdrAuditService {

    private final AdrRepository adrRepository;
    private final AuditEventRepository auditEventRepository;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    public List<AuditEventDto> getAuditLog(UUID adrId) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        return auditEventRepository.findAdrAuditEvents(workspaceId, adrId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private AuditEventDto toDto(AuditEvent event) {
        String type = resolveType(event.getAction());
        Map<String, Object> payload = buildPayload(event, type);
        return new AuditEventDto(
                event.getId(),
                type,
                event.getUser() != null ? event.getUser().getFullName() : "System",
                resolveAction(type, payload),
                event.getCreatedAt() != null ? event.getCreatedAt().toString() : null,
                resolveDetail(type, payload),
                payload.isEmpty() ? null : payload
        );
    }

    private String resolveType(String action) {
        return switch (action) {
            case "ADR_STATUS_CHANGED", "STATUS_CHANGED" -> "STATUS_CHANGED";
            case "ADR_CREATED" -> "ADR_CREATED";
            case "ADR_UPDATED" -> "ADR_UPDATED";
            case "VOTE_CAST" -> "VOTE_CAST";
            case "COMMENT_ADDED" -> "COMMENT_ADDED";
            default -> action;
        };
    }

    private String resolveAction(String type, Map<String, Object> payload) {
        return switch (type) {
            case "STATUS_CHANGED" -> {
                Object to = payload.get("to");
                yield to == null ? "changed ADR status" : "changed status to " + to;
            }
            case "VOTE_CAST" -> {
                Object vote = payload.get("vote");
                yield vote == null
                        ? "cast a vote"
                        : "voted " + vote.toString().toLowerCase(Locale.ROOT);
            }
            case "ADR_CREATED" -> "created this ADR";
            case "ADR_UPDATED" -> "updated this ADR";
            case "COMMENT_ADDED" -> "added a comment";
            default -> type;
        };
    }

    private String resolveDetail(String type, Map<String, Object> payload) {
        if ("STATUS_CHANGED".equals(type)) {
            Object from = payload.get("from");
            Object to = payload.get("to");
            if (from != null && to != null) {
                return "Status: " + from + " -> " + to;
            }
        }

        if ("VOTE_CAST".equals(type)) {
            Object comment = payload.get("comment");
            if (comment != null && !comment.toString().isBlank()) {
                return "Comment: " + comment;
            }
        }

        return null;
    }

    private Map<String, Object> buildPayload(AuditEvent event, String type) {
        Map<String, Object> oldJson = parseMap(event.getOldValueJson());
        Map<String, Object> newJson = parseMap(event.getNewValueJson());

        if ("STATUS_CHANGED".equals(type)) {
            Map<String, Object> payload = new LinkedHashMap<>();
            Object from = oldJson.getOrDefault("status", oldJson.get("from"));
            Object to = newJson.getOrDefault("status", newJson.get("to"));

            if (from != null) {
                payload.put("from", from);
            }
            if (to != null) {
                payload.put("to", to);
            }
            if (newJson.containsKey("reason")) {
                payload.put("reason", newJson.get("reason"));
            }
            return payload;
        }

        if (!newJson.isEmpty()) {
            return newJson;
        }

        return oldJson;
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of("raw", json);
        }
    }
}
