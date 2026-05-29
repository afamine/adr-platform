package com.adrplatform.adr.service;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AdrStatus;
import com.adrplatform.adr.dto.AdrDto;
import com.adrplatform.adr.dto.AdrSummaryDto;
import com.adrplatform.adr.dto.CreateAdrRequest;
import com.adrplatform.adr.dto.StatusTransitionRequest;
import com.adrplatform.adr.dto.UpdateAdrRequest;
import com.adrplatform.adr.exception.AdrAccessDeniedException;
import com.adrplatform.adr.exception.AdrNotFoundException;
import com.adrplatform.adr.exception.InvalidTransitionException;
import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.auth.domain.AuditEvent;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.repository.AuditEventRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.auth.service.AuditService;
import com.adrplatform.auth.config.CacheConfig;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.common.AuditActions;
import org.springframework.cache.annotation.CacheEvict;
import com.adrplatform.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdrService {

    private final AdrRepository adrRepository;
    private final AuditEventRepository auditEventRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<AdrDto> getAllAdrs(AdrStatus status, String search, String tag, Pageable pageable) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        if (workspaceId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Workspace context not available");
        }
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        String normalizedTag = (tag == null || tag.isBlank()) ? null : tag.trim().toLowerCase();
        String statusStr = status != null ? status.name() : null;
        return adrRepository.searchPaged(workspaceId, statusStr, normalizedSearch, normalizedTag, pageable)
                .map(AdrDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<AdrDto> getAllAdrs(AdrStatus status, String search, String tag) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        if (workspaceId == null) {
            log.error("TenantContext.workspaceId is null — JWT filter may not be populating it correctly");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Workspace context not available");
        }
        String normalizedTag = (tag == null || tag.isBlank()) ? null : tag.trim().toLowerCase();
        boolean noFilters = status == null && (search == null || search.isBlank()) && normalizedTag == null;
        List<Adr> list = noFilters
                ? adrRepository.findAllByWorkspace_IdOrderByAdrNumberDesc(workspaceId)
                : adrRepository.search(workspaceId, status != null ? status.name() : null,
                        (search == null || search.isBlank()) ? null : search.trim(), normalizedTag);
        return list.stream().map(AdrDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public AdrDto getAdrById(UUID id) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        Adr adr = adrRepository.findByIdAndWorkspace_Id(id, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));
        return AdrDto.fromEntity(adr);
    }

    @Transactional(readOnly = true)
    public Adr getAdrEntity(UUID id) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        return adrRepository.findByIdAndWorkspace_Id(id, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));
    }

    @Transactional(readOnly = true)
    public List<AdrDto> getRecentAdrs(int limit) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        return adrRepository.findRecentByWorkspace(workspaceId, PageRequest.of(0, limit))
                .stream()
                .map(adr -> {
                    List<AuditEvent> events = auditEventRepository.findAdrAuditEvents(workspaceId, adr.getId());
                    if (events.isEmpty()) {
                        return AdrDto.fromEntityWithActivity(adr, "Created",
                                java.time.LocalDateTime.ofInstant(adr.getCreatedAt(), java.time.ZoneOffset.UTC));
                    }
                    AuditEvent latest = events.get(0);
                    String action = resolveLastAction(latest.getAction(), latest.getNewValueJson());
                    java.time.LocalDateTime actionDate =
                            java.time.LocalDateTime.ofInstant(latest.getCreatedAt(), java.time.ZoneOffset.UTC);
                    return AdrDto.fromEntityWithActivity(adr, action, actionDate);
                })
                .toList();
    }

    private String resolveLastAction(String action, String newValueJson) {
        if (action == null) return "Created";
        return switch (action) {
            case "STATUS_CHANGED" -> {
                if (newValueJson != null) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode node =
                            objectMapper.readTree(newValueJson);
                        if (node.has("status")) {
                            yield node.get("status").asText("Status changed");
                        }
                    } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
                        // fall through to default
                    }
                }
                yield "Status changed";
            }
            case "VOTE_CAST"   -> "Vote cast";
            case "ADR_UPDATED" -> "Updated";
            case "ADR_CREATED" -> "Created";
            default -> action.replace("_", " ").toLowerCase();
        };
    }

    @Transactional
    public AdrDto createAdr(CreateAdrRequest request) {
        User actor = currentUser();
        if (!(actor.getRole() == Role.ADMIN || actor.getRole() == Role.AUTHOR)) {
            throw new AdrAccessDeniedException("You don't have permission to create ADRs.");
        }
        UUID workspaceId = tenantContext.getWorkspaceId();
        workspaceRepository.findByIdWithLock(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Workspace not found"));
        int nextNumber = adrRepository.nextAdrNumber(workspaceId);

        log.debug("Saving ADR with tags: {}", request.tags());

        Adr adr = Adr.builder()
                .workspace(actor.getWorkspace())
                .adrNumber(nextNumber)
                .title(request.title().trim())
                .status(AdrStatus.DRAFT)
                .context(request.context())
                .decision(request.decision())
                .consequences(request.consequences())
                .alternatives(request.alternatives())
                .tagsCsv(joinTags(request.tags()))
                .author(actor)
                .build();
        Adr saved = adrRepository.save(adr);

        log.debug("Loaded ADR tags raw: '{}', parsed: {}", saved.getTagsCsv(), AdrDto.fromEntity(saved).tags());

        auditService.record(actor, actor.getWorkspace(), AuditActions.ADR_CREATED, "ADR", saved.getId(), null,
                toJson(Map.of("adrNumber", saved.getAdrNumber(), "title",
                        saved.getTitle() == null ? "" : saved.getTitle(), "status", saved.getStatus().name())));

        log.info("ADR created id={} number={} by {}", saved.getId(), saved.getAdrNumber(), actor.getEmail());
        return AdrDto.fromEntity(saved);
    }

    @CacheEvict(value = CacheConfig.AI_INSIGHTS_CACHE, allEntries = true)
    @Transactional
    public AdrDto updateAdr(UUID id, UpdateAdrRequest request) {
        User actor = currentUser();
        UUID workspaceId = tenantContext.getWorkspaceId();
        Adr adr = adrRepository.findByIdAndWorkspace_Id(id, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        if (actor.getRole() != Role.ADMIN) {
            boolean isOwner = adr.getAuthor().getId().equals(actor.getId());
            if (!isOwner || !(adr.getStatus() == AdrStatus.DRAFT || adr.getStatus() == AdrStatus.PROPOSED)) {
                throw new AdrAccessDeniedException("You don't have permission to modify this ADR.");
            }
        }
        if (actor.getRole() != Role.ADMIN && (adr.getStatus() == AdrStatus.ACCEPTED || adr.getStatus() == AdrStatus.REJECTED)) {
            throw new BadRequestException("Cannot edit a closed ADR.");
        }

        if (request.title() != null && !request.title().isBlank()) adr.setTitle(request.title().trim());
        if (request.context() != null) adr.setContext(request.context());
        if (request.decision() != null) adr.setDecision(request.decision());
        if (request.consequences() != null) adr.setConsequences(request.consequences());
        if (request.alternatives() != null) adr.setAlternatives(request.alternatives());
        if (request.tags() != null && !request.tags().isEmpty()) adr.setTagsCsv(joinTags(request.tags()));

        Adr saved = adrRepository.save(adr);
        auditService.record(actor, actor.getWorkspace(), AuditActions.ADR_UPDATED, "ADR", saved.getId(), null,
                toJson(Map.of("title", saved.getTitle() == null ? "" : saved.getTitle())));
        return AdrDto.fromEntity(saved);
    }

    @CacheEvict(value = CacheConfig.AI_INSIGHTS_CACHE, allEntries = true)
    @Transactional
    public AdrDto transitionStatus(UUID id, StatusTransitionRequest request) {
        AdrStatus newStatus = request.status();
        User actor = currentUser();
        UUID workspaceId = tenantContext.getWorkspaceId();
        Adr adr = adrRepository.findByIdAndWorkspace_Id(id, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        AdrStatus old = adr.getStatus();
        if (old == newStatus) {
            return AdrDto.fromEntity(adr); // no-op
        }

        enforceTransition(actor, adr, newStatus);

        adr.setStatus(newStatus);
        if (newStatus == AdrStatus.UNDER_REVIEW && adr.getReviewStartedAt() == null) {
            adr.setReviewStartedAt(java.time.Instant.now());
        }

        if (newStatus == AdrStatus.SUPERSEDED && request.supersededByAdrId() != null) {
            Adr replacement = adrRepository.findByIdAndWorkspace_Id(request.supersededByAdrId(), workspaceId)
                    .orElseThrow(() -> new AdrNotFoundException("Replacement ADR not found in this workspace."));
            if (replacement.getId().equals(adr.getId())) {
                throw new InvalidTransitionException("An ADR cannot supersede itself.");
            }
            adr.setSupersededBy(replacement);
            replacement.setSupersedes(adr);
            adrRepository.save(replacement);
        }

        Adr saved = adrRepository.save(adr);

        auditService.record(actor, actor.getWorkspace(), AuditActions.ADR_STATUS_CHANGED, "ADR", saved.getId(),
                toJson(Map.of("status", old.name())),
                toJson(Map.of("status", newStatus.name(),
                        "supersededByAdrId", request.supersededByAdrId() != null
                                ? request.supersededByAdrId().toString() : "")));
        notificationService.notifyStatusChanged(saved.getId(), saved.getAdrNumber(), saved.getTitle(),
                workspaceId, saved.getAuthor().getId(), actor.getId(), newStatus.name());
        return AdrDto.fromEntity(saved);
    }

    @Transactional
    public void deleteAdr(UUID id) {
        User actor = currentUser();
        if (actor.getRole() != Role.ADMIN) {
            throw new AdrAccessDeniedException("You don't have permission to modify this ADR.");
        }
        UUID workspaceId = tenantContext.getWorkspaceId();
        Adr adr = adrRepository.findByIdAndWorkspace_Id(id, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        auditService.record(actor, actor.getWorkspace(), AuditActions.ADR_DELETED, "ADR", adr.getId(), null, null);
        adrRepository.delete(adr);
    }

    private void enforceTransition(User actor, Adr adr, AdrStatus target) {
        AdrStatus current = adr.getStatus();
        Role role = actor.getRole();
        boolean isOwner = adr.getAuthor().getId().equals(actor.getId());

        if (role == Role.ADMIN) return; // ADMIN overrides all

        switch (current) {
            case DRAFT -> {
                if (target == AdrStatus.PROPOSED) {
                    if (!(role == Role.AUTHOR && isOwner)) throw new AdrAccessDeniedException("Only the author can propose this ADR.");
                } else {
                    throw new InvalidTransitionException("Invalid status transition from " + current + " to " + target + ".");
                }
            }
            case PROPOSED -> {
                if (target == AdrStatus.UNDER_REVIEW) {
                    if (!(role == Role.REVIEWER)) throw new AdrAccessDeniedException("You don't have permission to modify this ADR.");
                } else if (target == AdrStatus.DRAFT) {
                    if (!(isOwner && role == Role.AUTHOR)) throw new AdrAccessDeniedException("You don't have permission to modify this ADR.");
                } else {
                    throw new InvalidTransitionException("Invalid status transition from " + current + " to " + target + ".");
                }
            }
            case UNDER_REVIEW -> {
                if (target == AdrStatus.ACCEPTED || target == AdrStatus.REJECTED) {
                    if (!(role == Role.APPROVER)) throw new AdrAccessDeniedException("You don't have permission to modify this ADR.");
                } else {
                    throw new InvalidTransitionException("Invalid status transition from " + current + " to " + target + ".");
                }
            }
            case ACCEPTED -> {
                if (target == AdrStatus.SUPERSEDED) {
                    if (!(role == Role.APPROVER || role == Role.ADMIN)) {
                        throw new AdrAccessDeniedException("Only APPROVER or ADMIN can supersede an ADR.");
                    }
                } else {
                    throw new InvalidTransitionException("Invalid status transition from " + current + " to " + target + ".");
                }
            }
            case REJECTED -> {
                if (target == AdrStatus.DRAFT) {
                    if (!(role == Role.AUTHOR && isOwner)) throw new AdrAccessDeniedException("You don't have permission to modify this ADR.");
                } else {
                    throw new InvalidTransitionException("Invalid status transition from " + current + " to " + target + ".");
                }
            }
            case SUPERSEDED -> throw new InvalidTransitionException("Invalid status transition from " + current + " to " + target + ".");
        }
    }

    @Transactional(readOnly = true)
    public List<AdrSummaryDto> getEligibleReplacements() {
        UUID workspaceId = tenantContext.getWorkspaceId();
        return adrRepository.findEligibleReplacements(workspaceId)
                .stream()
                .map(a -> new AdrSummaryDto(a.getId(), a.getAdrNumber(), a.getTitle(), a.getStatus().name()))
                .toList();
    }

    public String buildMadrMarkdown(AdrDto adr) {
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = adr.createdAt() != null ? adr.createdAt().format(dateFmt) : "—";
        String tags = (adr.tags() == null || adr.tags().isEmpty())
                ? "—"
                : String.join(", ", adr.tags());

        return new StringBuilder()
                .append("# ADR-").append(adr.adrNumber()).append(": ").append(adr.title()).append("\n\n")
                .append("**Status:** ").append(adr.status()).append("\n")
                .append("**Date:** ").append(date).append("\n")
                .append("**Author:** ").append(adr.authorName()).append("\n")
                .append("**Tags:** ").append(tags).append("\n\n")
                .append("---\n\n")
                .append("## Context\n\n")
                .append(isBlank(adr.context()) ? "_Not provided._" : adr.context().trim()).append("\n\n")
                .append("## Decision\n\n")
                .append(isBlank(adr.decision()) ? "_Not provided._" : adr.decision().trim()).append("\n\n")
                .append("## Consequences\n\n")
                .append(isBlank(adr.consequences()) ? "_Not provided._" : adr.consequences().trim()).append("\n\n")
                .append("## Alternatives Considered\n\n")
                .append(isBlank(adr.alternatives()) ? "_Not provided._" : adr.alternatives().trim()).append("\n\n")
                .append("---\n")
                .append("*Generated by ADR Platform*\n")
                .toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        String joined = String.join(",", tags.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        return "," + joined + ",";
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit payload", ex);
        }
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
