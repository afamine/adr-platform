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
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.auth.service.AuditService;
import com.adrplatform.auth.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdrService {

    private final AdrRepository adrRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<AdrDto> getAllAdrs(AdrStatus status, String search) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        List<Adr> list = (status == null && (search == null || search.isBlank()))
                ? adrRepository.findAllByWorkspace_IdOrderByAdrNumberDesc(workspaceId)
                : adrRepository.search(workspaceId, status, (search == null || search.isBlank()) ? null : search.trim());
        return list.stream().map(AdrDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public AdrDto getAdrById(UUID id) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        Adr adr = adrRepository.findByIdAndWorkspace_Id(id, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));
        return AdrDto.fromEntity(adr);
    }

    @Transactional
    public AdrDto createAdr(CreateAdrRequest request) {
        User actor = currentUser();
        if (!(actor.getRole() == Role.ADMIN || actor.getRole() == Role.AUTHOR)) {
            throw new AdrAccessDeniedException("You don't have permission to create ADRs.");
        }
        UUID workspaceId = tenantContext.getWorkspaceId();
        int nextNumber = adrRepository.findMaxAdrNumber(workspaceId) + 1;

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

        String createdJson = "{" +
                "\"adrNumber\":" + saved.getAdrNumber() + "," +
                "\"title\":\"" + (saved.getTitle() == null ? "" : saved.getTitle().replace("\"", "\\\"")) + "\"," +
                "\"status\":\"" + saved.getStatus().name() + "\"" +
                "}";
        auditService.record(actor, actor.getWorkspace(), "ADR_CREATED", "ADR", saved.getId(), null, createdJson);

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
        if (request.tags() != null) adr.setTagsCsv(joinTags(request.tags()));

        Adr saved = adrRepository.save(adr);
        String updatedJson = "{" +
                "\"title\":\"" + (saved.getTitle() == null ? "" : saved.getTitle().replace("\"", "\\\"")) + "\"" +
                "}";
        auditService.record(actor, actor.getWorkspace(), "ADR_UPDATED", "ADR", saved.getId(), null, updatedJson);
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
        Adr saved = adrRepository.save(adr);

        String oldJson = "{\"status\":\"" + old.name() + "\"}";
        String newJson = "{\"status\":\"" + newStatus.name() + "\"}";
        auditService.record(actor, actor.getWorkspace(), "ADR_STATUS_CHANGED", "ADR", saved.getId(), oldJson, newJson);
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
                    if (!(role == Role.AUTHOR)) throw new AdrAccessDeniedException("You don't have permission to modify this ADR.");
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
        if (tags == null || tags.isEmpty()) return null;
        return String.join(",", tags.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
