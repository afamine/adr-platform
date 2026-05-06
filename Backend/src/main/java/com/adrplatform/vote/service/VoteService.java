package com.adrplatform.vote.service;

import com.adrplatform.adr.domain.Adr;
import com.adrplatform.adr.domain.AdrStatus;
import com.adrplatform.adr.exception.AdrAccessDeniedException;
import com.adrplatform.adr.exception.AdrNotFoundException;
import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.auth.service.AuditService;
import com.adrplatform.vote.domain.Vote;
import com.adrplatform.vote.domain.VoteType;
import com.adrplatform.vote.dto.CastVoteRequest;
import com.adrplatform.vote.dto.VoteDto;
import com.adrplatform.vote.exception.AlreadyVotedException;
import com.adrplatform.vote.exception.InvalidVoteException;
import com.adrplatform.vote.repository.VoteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class VoteService {

    private final VoteRepository voteRepository;
    private final AdrRepository adrRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public VoteDto castVote(UUID adrId, CastVoteRequest request, UUID voterId) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        User voter = currentUser();

        if (!voter.getId().equals(voterId)) {
            log.warn("Vote cast voterId mismatch. tokenUserId={} requestedVoterId={}", voter.getId(), voterId);
        }

        Adr adr = adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        if (adr.getStatus() != AdrStatus.UNDER_REVIEW) {
            throw new InvalidVoteException("This ADR is not under review.");
        }

        Role role = voter.getRole();
        if (role == Role.AUTHOR) {
            throw new AdrAccessDeniedException("Authors cannot vote on ADRs.");
        }
        if (!Set.of(Role.REVIEWER, Role.APPROVER, Role.ADMIN).contains(role)) {
            throw new AdrAccessDeniedException("You don't have permission to vote on this ADR.");
        }

        if (voteRepository.existsByAdrIdAndVoterId(adrId, voter.getId())) {
            throw new AlreadyVotedException("You have already voted on this ADR.");
        }

        String normalizedComment = request.comment().trim();
        Vote saved = voteRepository.save(Vote.builder()
                .adrId(adr.getId())
                .workspaceId(workspaceId)
                .voterId(voter.getId())
                .voteType(request.voteType())
                .comment(normalizedComment)
                .build());

        auditService.record(
                voter,
                voter.getWorkspace(),
                "VOTE_CAST",
                "ADR",
                adr.getId(),
                null,
                toJson(Map.of("vote", request.voteType().name(), "comment", normalizedComment))
        );

        AdrStatus previousStatus = adr.getStatus();
        String transitionReason = null;

        if (role == Role.APPROVER || role == Role.ADMIN) {
            AdrStatus finalStatus = request.voteType() == VoteType.APPROVE ? AdrStatus.ACCEPTED : AdrStatus.REJECTED;
            adr.setStatus(finalStatus);
            adrRepository.save(adr);
            transitionReason = "Approver final decision";
        } else {
            transitionReason = checkQuorumAndTransition(adr, adr.getWorkspace());
        }

        if (transitionReason != null && previousStatus != adr.getStatus()) {
            auditService.record(
                    voter,
                    voter.getWorkspace(),
                    "STATUS_CHANGED",
                    "ADR",
                    adr.getId(),
                    toJson(Map.of("status", previousStatus.name())),
                    toJson(Map.of("status", adr.getStatus().name(), "reason", transitionReason))
            );
        }

        return new VoteDto(
                saved.getId(),
                saved.getAdrId(),
                saved.getVoterId(),
                voter.getFullName(),
                voter.getRole().name(),
                saved.getVoteType(),
                saved.getComment(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<VoteDto> getVotesForAdr(UUID adrId) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        List<Vote> votes = voteRepository.findAllByAdrIdAndWorkspaceIdOrderByCreatedAtDesc(adrId, workspaceId);

        Map<UUID, User> votersById = userRepository.findAllById(
                        votes.stream().map(Vote::getVoterId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return votes.stream().map(vote -> {
            User voter = votersById.get(vote.getVoterId());
            return new VoteDto(
                    vote.getId(),
                    vote.getAdrId(),
                    vote.getVoterId(),
                    voter != null ? voter.getFullName() : "Unknown user",
                    voter != null ? voter.getRole().name() : "UNKNOWN",
                    vote.getVoteType(),
                    vote.getComment(),
                    vote.getCreatedAt()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public VoteDto getMyVote(UUID adrId, UUID voterId) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        User voter = currentUser();
        UUID effectiveVoterId = voter.getId().equals(voterId) ? voterId : voter.getId();

        Vote vote = voteRepository.findByAdrIdAndWorkspaceIdAndVoterId(adrId, workspaceId, effectiveVoterId)
                .orElseThrow(() -> new ResourceNotFoundException("Vote not found for current user."));

        return new VoteDto(
                vote.getId(),
                vote.getAdrId(),
                vote.getVoterId(),
                voter.getFullName(),
                voter.getRole().name(),
                vote.getVoteType(),
                vote.getComment(),
                vote.getCreatedAt()
        );
    }

    private String checkQuorumAndTransition(Adr adr, Workspace workspace) {
        Integer configuredQuorum = workspace.getVoteQuorum();
        int quorum = configuredQuorum == null ? 2 : configuredQuorum;

        if (quorum <= 0) {
            return null;
        }

        long approveCount = voteRepository.countByAdrIdAndWorkspaceIdAndVoteType(
                adr.getId(),
                workspace.getId(),
                VoteType.APPROVE
        );
        long rejectCount = voteRepository.countByAdrIdAndWorkspaceIdAndVoteType(
                adr.getId(),
                workspace.getId(),
                VoteType.REJECT
        );

        if (approveCount >= quorum) {
            adr.setStatus(AdrStatus.ACCEPTED);
            adrRepository.save(adr);
            return "Quorum reached";
        }

        if (rejectCount >= quorum) {
            adr.setStatus(AdrStatus.REJECTED);
            adrRepository.save(adr);
            return "Quorum reached";
        }

        return null;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit payload.", ex);
        }
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
