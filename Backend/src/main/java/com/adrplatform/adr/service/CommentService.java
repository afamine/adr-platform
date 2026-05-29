package com.adrplatform.adr.service;

import com.adrplatform.adr.domain.AdrComment;
import com.adrplatform.adr.dto.CommentDto;
import com.adrplatform.adr.dto.CreateCommentRequest;
import com.adrplatform.adr.dto.HistoryEventDto;
import com.adrplatform.adr.dto.TeamMemberDto;
import com.adrplatform.adr.exception.AdrAccessDeniedException;
import com.adrplatform.adr.exception.AdrNotFoundException;
import com.adrplatform.adr.repository.AdrCommentRepository;
import com.adrplatform.adr.repository.AdrRepository;
import com.adrplatform.auth.domain.AuditEvent;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.repository.AuditEventRepository;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.security.TenantContext;
import com.adrplatform.auth.service.AuditService;
import com.adrplatform.common.AuditActions;
import com.adrplatform.notification.service.NotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final AdrCommentRepository commentRepository;
    private final AdrRepository adrRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(UUID adrId) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        List<AdrComment> comments = commentRepository
                .findByAdrIdAndWorkspaceIdOrderByCreatedAtAsc(adrId, workspaceId);

        Set<UUID> authorIds = comments.stream()
                .map(AdrComment::getAuthorId)
                .collect(Collectors.toSet());

        Map<UUID, User> usersById = userRepository.findAllById(authorIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream()
                .map(c -> toCommentDto(c, usersById.get(c.getAuthorId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto addComment(UUID adrId, CreateCommentRequest request) {
        User actor = currentUser();
        UUID workspaceId = tenantContext.getWorkspaceId();
        var adr = adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        AdrComment comment = AdrComment.builder()
                .adrId(adrId)
                .workspaceId(workspaceId)
                .authorId(actor.getId())
                .content(request.content().trim())
                .build();
        AdrComment saved = commentRepository.save(comment);

        auditService.record(actor, actor.getWorkspace(), AuditActions.COMMENT_ADDED, "ADR", adrId, null, null);
        notificationService.notifyCommentAdded(
                adrId, adr.getAdrNumber(), adr.getTitle(),
                workspaceId, adr.getAuthor().getId(), actor.getId());
        log.info("Comment added to ADR {} by {}", adrId, actor.getEmail());

        return toCommentDto(saved, actor);
    }

    @Transactional
    public CommentDto resolveComment(UUID adrId, UUID commentId, boolean resolved) {
        User actor = currentUser();
        UUID workspaceId = tenantContext.getWorkspaceId();
        adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        AdrComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));

        if (!comment.getAdrId().equals(adrId)) {
            throw new ResourceNotFoundException("Comment not found.");
        }

        if (actor.getRole() != Role.ADMIN && !comment.getAuthorId().equals(actor.getId())) {
            throw new AdrAccessDeniedException("You don't have permission to resolve this comment.");
        }

        comment.setResolved(resolved);
        comment.setResolvedBy(resolved ? actor.getId() : null);
        comment.setResolvedAt(resolved ? LocalDateTime.now() : null);
        AdrComment saved = commentRepository.save(comment);

        User author = userRepository.findById(saved.getAuthorId()).orElse(null);
        return toCommentDto(saved, author);
    }

    @Transactional(readOnly = true)
    public List<HistoryEventDto> getHistory(UUID adrId) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        return auditEventRepository.findAdrAuditEvents(workspaceId, adrId)
                .stream()
                .map(this::toHistoryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeamMemberDto> getTeam(UUID adrId) {
        UUID workspaceId = tenantContext.getWorkspaceId();
        adrRepository.findByIdAndWorkspace_Id(adrId, workspaceId)
                .orElseThrow(() -> new AdrNotFoundException("ADR not found."));

        return userRepository.findAllByWorkspace_Id(workspaceId)
                .stream()
                .filter(User::isActive)
                .map(this::toTeamMemberDto)
                .collect(Collectors.toList());
    }

    private CommentDto toCommentDto(AdrComment c, User author) {
        String name = author != null ? author.getFullName() : "Unknown";
        String initials = buildInitials(name);
        return new CommentDto(
                c.getId(),
                c.getAdrId(),
                c.getAuthorId(),
                name,
                initials,
                c.getContent(),
                c.isResolved(),
                formatLocalTimeAgo(c.getCreatedAt()),
                formatLocalTimeAgo(c.getUpdatedAt())
        );
    }

    private HistoryEventDto toHistoryDto(AuditEvent event) {
        String actor = event.getUser() != null ? event.getUser().getFullName() : "System";
        String action = buildHistoryAction(event);
        String timeAgo = formatInstantTimeAgo(event.getCreatedAt());
        return new HistoryEventDto(actor, action, timeAgo, event.getAction());
    }

    private TeamMemberDto toTeamMemberDto(User user) {
        return new TeamMemberDto(
                user.getId(),
                user.getFullName(),
                buildInitials(user.getFullName()),
                user.getRole().name(),
                user.getAvatarColor()
        );
    }

    private String buildHistoryAction(AuditEvent event) {
        return switch (event.getAction()) {
            case "ADR_STATUS_CHANGED", "STATUS_CHANGED" -> {
                String status = extractJsonField(event.getNewValueJson(), "status");
                yield status != null ? "updated status to " + status : "updated status";
            }
            case "ADR_CREATED" -> "created this ADR";
            case "ADR_UPDATED" -> "updated this ADR";
            case "VOTE_CAST" -> {
                String vote = extractJsonField(event.getNewValueJson(), "vote");
                yield vote != null ? "voted " + vote.toLowerCase() : "cast a vote";
            }
            case "COMMENT_ADDED" -> "added a comment";
            default -> event.getAction().replace("_", " ").toLowerCase();
        };
    }

    private String extractJsonField(String json, String field) {
        if (json == null || json.isBlank()) return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object val = map.get(field);
            return val != null ? val.toString() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String initials = Arrays.stream(fullName.trim().split("\\s+"))
                .filter(w -> !w.isEmpty())
                .map(w -> String.valueOf(w.charAt(0)))
                .collect(Collectors.joining())
                .toUpperCase();
        return initials.length() > 2 ? initials.substring(0, 2) : initials;
    }

    private String formatLocalTimeAgo(LocalDateTime dt) {
        if (dt == null) return "";
        long minutes = ChronoUnit.MINUTES.between(dt, LocalDateTime.now());
        if (minutes < 2) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days == 1) return "1 day ago";
        return days + " days ago";
    }

    private String formatInstantTimeAgo(Instant instant) {
        if (instant == null) return "";
        long minutes = ChronoUnit.MINUTES.between(instant, Instant.now());
        if (minutes < 2) return "just now";
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days == 1) return "1d ago";
        return days + "d ago";
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
