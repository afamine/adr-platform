package com.adrplatform.notification.service;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.repository.NotificationPreferencesRepository;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.notification.domain.Notification;
import com.adrplatform.notification.dto.NotificationDto;
import com.adrplatform.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferencesRepository notificationPreferencesRepository;

    // ── Query methods (called by NotificationController) ─────────────────────

    @Transactional(readOnly = true)
    public List<NotificationDto> getNotifications(UUID userId, int limit, boolean unreadOnly) {
        List<Notification> results = unreadOnly
                ? notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
        return results.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public long markAllRead(UUID userId) {
        long count = notificationRepository.countByRecipientIdAndIsReadFalse(userId);
        notificationRepository.markAllReadForUser(userId);
        return count;
    }

    // ── Event hooks (called asynchronously from domain services) ─────────────

    /**
     * Called after an ADR status transition.
     * UNDER_REVIEW → notifies all REVIEWERs and APPROVERs.
     * ACCEPTED / REJECTED → notifies the ADR author.
     * All parameters are pre-extracted in the caller's transaction to avoid
     * lazy-loading issues in the async thread.
     */
    @Async
    @Transactional
    public void notifyStatusChanged(UUID adrId, int adrNumber, String adrTitle,
            UUID workspaceId, UUID authorId, UUID actorId, String newStatus) {
        try {
            String adrRef = "ADR-" + adrNumber;
            switch (newStatus) {
                case "UNDER_REVIEW" -> {
                    List<Notification> notifications = userRepository.findAllByWorkspace_Id(workspaceId)
                            .stream()
                            .filter(u -> (u.getRole() == Role.REVIEWER || u.getRole() == Role.APPROVER)
                                    && !u.getId().equals(actorId)
                                    && shouldNotify(u.getId(), "REVIEW"))
                            .map(u -> buildNotification(u.getId(), workspaceId,
                                    "ADR_SUBMITTED_FOR_REVIEW",
                                    "ADR Submitted for Review",
                                    adrRef + " — " + adrTitle + " was submitted for your review",
                                    adrId, null))
                            .toList();
                    if (!notifications.isEmpty()) notificationRepository.saveAll(notifications);
                }
                case "ACCEPTED" -> {
                    if (!authorId.equals(actorId) && shouldNotify(authorId, "STATUS")) {
                        notificationRepository.save(buildNotification(authorId, workspaceId,
                                "ADR_STATUS_CHANGED",
                                "ADR Accepted",
                                adrRef + " — " + adrTitle + " has been accepted",
                                adrId, null));
                    }
                }
                case "REJECTED" -> {
                    if (!authorId.equals(actorId) && shouldNotify(authorId, "STATUS")) {
                        notificationRepository.save(buildNotification(authorId, workspaceId,
                                "ADR_REJECTED",
                                "ADR Rejected",
                                adrRef + " — " + adrTitle + " was rejected",
                                adrId, null));
                    }
                }
                default -> { /* no notification for other transitions */ }
            }
        } catch (Exception ex) {
            log.error("Failed to create status-changed notifications for ADR {}: {}", adrId, ex.getMessage(), ex);
        }
    }

    /**
     * Called after a vote is cast. Notifies the ADR author (unless they cast the vote themselves).
     */
    @Async
    @Transactional
    public void notifyVoteCast(UUID adrId, int adrNumber, String adrTitle,
            UUID workspaceId, UUID authorId, UUID voterId) {
        try {
            if (authorId.equals(voterId)) return;
            if (!shouldNotify(authorId, "VOTE")) return;
            String voterName = userRepository.findById(voterId)
                    .map(User::getFullName).orElse("Someone");
            notificationRepository.save(buildNotification(authorId, workspaceId,
                    "VOTE_CAST_ON_MY_ADR",
                    "Vote Cast on Your ADR",
                    voterName + " voted on ADR-" + adrNumber + " — " + adrTitle,
                    adrId, null));
        } catch (Exception ex) {
            log.error("Failed to create vote-cast notification for ADR {}: {}", adrId, ex.getMessage(), ex);
        }
    }

    /**
     * Called after a comment is added to an ADR. Notifies the ADR author
     * (unless the commenter is the author themselves).
     */
    @Async
    @Transactional
    public void notifyCommentAdded(UUID adrId, int adrNumber, String adrTitle,
            UUID workspaceId, UUID authorId, UUID commenterId) {
        try {
            if (authorId.equals(commenterId)) return;
            if (!shouldNotify(authorId, "COMMENT_ADDED")) return;
            String commenterName = userRepository.findById(commenterId)
                    .map(User::getFullName).orElse("Someone");
            notificationRepository.save(buildNotification(authorId, workspaceId,
                    "COMMENT_ADDED",
                    "New Comment on Your ADR",
                    commenterName + " commented on ADR-" + adrNumber + " — " + adrTitle,
                    adrId, null));
        } catch (Exception ex) {
            log.error("Failed to create comment-added notification for ADR {}: {}", adrId, ex.getMessage(), ex);
        }
    }

    /**
     * Called after a new user joins the workspace. Notifies all ADMIN users.
     */
    @Async
    @Transactional
    public void notifyNewTeamMember(UUID newUserId, UUID workspaceId) {
        try {
            User newUser = userRepository.findById(newUserId).orElse(null);
            if (newUser == null) return;
            String name = newUser.getFullName();
            String roleName = newUser.getRole().name();
            List<Notification> notifications = userRepository.findAllByWorkspace_Id(workspaceId)
                    .stream()
                    .filter(u -> u.getRole() == Role.ADMIN && !u.getId().equals(newUserId))
                    .map(u -> buildNotification(u.getId(), workspaceId,
                            "NEW_TEAM_MEMBER",
                            "New Team Member",
                            name + " joined the workspace as " + roleName,
                            null, null))
                    .toList();
            if (!notifications.isEmpty()) notificationRepository.saveAll(notifications);
        } catch (Exception ex) {
            log.error("Failed to create new-team-member notifications for user {}: {}", newUserId, ex.getMessage(), ex);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Checks whether a specific notification type should be sent to a user,
     * based on their stored NotificationPreferences.
     *
     * If no preferences record exists for the user, defaults to TRUE (opt-out model):
     * users receive notifications until they explicitly disable them.
     *
     * @param recipientId The UUID of the notification recipient
     * @param type        One of: "REVIEW", "VOTE", "STATUS"
     * @return true if the notification should be created; false if the user opted out
     */
    private boolean shouldNotify(UUID recipientId, String type) {
        return notificationPreferencesRepository
                .findByUser_Id(recipientId)
                .map(prefs -> switch (type) {
                    case "REVIEW"  -> prefs.isEmailOnReview();
                    case "VOTE"    -> prefs.isEmailOnVote();
                    case "STATUS"  -> prefs.isEmailOnStatus();
                    default        -> true;
                })
                .orElse(true); // No preferences record → send by default (opt-out model)
    }

    private Notification buildNotification(UUID recipientId, UUID workspaceId,
            String type, String title, String body, UUID adrId, UUID auditEventId) {
        return Notification.builder()
                .recipientId(recipientId)
                .workspaceId(workspaceId)
                .type(type)
                .title(title)
                .body(body)
                .adrId(adrId)
                .auditEventId(auditEventId)
                .isRead(false)
                .build();
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getAdrId(),
                n.isRead(),
                n.getCreatedAt().toString(),
                timeAgo(n.getCreatedAt()));
    }

    public static String timeAgo(Instant instant) {
        long minutes = ChronoUnit.MINUTES.between(instant, Instant.now());
        if (minutes < 1)  return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24)   return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        long days = hours / 24;
        if (days == 1)    return "Yesterday";
        if (days < 7)     return days + " days ago";
        return LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
    }
}
