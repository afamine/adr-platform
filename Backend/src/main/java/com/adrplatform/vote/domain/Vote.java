package com.adrplatform.vote.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vote")
public class Vote {

    @Id
    private UUID id;

    @Column(name = "adr_id", nullable = false)
    private UUID adrId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
