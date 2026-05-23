package com.adrplatform.adr.service;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AdrStatus;
import com.adrplatform.adr.dto.AdrDto;
import com.adrplatform.adr.dto.CreateAdrRequest;
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
import com.adrplatform.auth.exception.BadRequestException;
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
    public Page<AdrDto> getAllAdrs(AdrStatus status, String search, Pageable pageable) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        if (workspaceId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Workspace context not available");
        }
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        String statusStr = status != null ? status.name() : null;
        return adrRepository.searchPaged(workspaceId, statusStr, normalizedSearch, pageable)
                .map(AdrDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<AdrDto> getAllAdrs(AdrStatus status, String search) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        if (workspaceId == null) {
            log.error("TenantContext.workspaceId is null — JWT filter may not be populating it correctly");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Workspace context not available");
        }
        List<Adr> list = (status == null && (search == null || search.isBlank()))
                ? adrRepository.findAllByWorkspace_IdOrderByAdrNumberDesc(workspaceId)
                : adrRepository.search(workspaceId, status != null ? status.name() : null, (search == null || search.isBlank()) ? null : search.trim());
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

        auditService.record(actor, actor.getWorkspace(), "ADR_CREATED", "ADR", saved.getId(), null,
                toJson(Map.of("adrNumber", saved.getAdrNumber(), "title",
                        saved.getTitle() == null ? "" : saved.getTitle(), "status", saved.getStatus().name())));

        log.info("ADR created id={} number={} by {}", saved.getId(), saved.getAdrNumber(), actor.getEmail());
        return AdrDto.fromEntity(saved);
    }

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
        auditService.record(actor, actor.getWorkspace(), "ADR_UPDATED", "ADR", saved.getId(), null,
                toJson(Map.of("title", saved.getTitle() == null ? "" : saved.getTitle())));
        return AdrDto.fromEntity(saved);
    }

    @Transactional
    public AdrDto transitionStatus(UUID id, AdrStatus newStatus) {
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
            adr.setReviewStartedAt(java.time.LocalDateTime.now());
        }
        Adr saved = adrRepository.save(adr);

        auditService.record(actor, actor.getWorkspace(), "ADR_STATUS_CHANGED", "ADR", saved.getId(),
                toJson(Map.of("status", old.name())),
                toJson(Map.of("status", newStatus.name())));
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

        auditService.record(actor, actor.getWorkspace(), "ADR_DELETED", "ADR", adr.getId(), null, null);
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
                if (target != AdrStatus.SUPERSEDED) {
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

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "";
        return String.join(",", tags.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
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
