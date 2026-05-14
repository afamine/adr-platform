package com.adrplatform.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "email_on_review", nullable = false)
    private boolean emailOnReview = true;

    @Builder.Default
    @Column(name = "email_on_vote", nullable = false)
    private boolean emailOnVote = true;

    @Builder.Default
    @Column(name = "email_on_status", nullable = false)
    private boolean emailOnStatus = true;

    @Builder.Default
    @Column(name = "slack_enabled", nullable = false)
    private boolean slackEnabled = false;

    @Column(name = "slack_webhook", length = 500)
    private String slackWebhook;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
