package com.adrplatform.vote.repository;

import com.adrplatform.vote.domain.Vote;
import com.adrplatform.vote.domain.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

    boolean existsByAdrIdAndVoterId(UUID adrId, UUID voterId);

    List<Vote> findAllByAdrIdAndWorkspaceIdOrderByCreatedAtDesc(UUID adrId, UUID workspaceId);

    Optional<Vote> findByAdrIdAndWorkspaceIdAndVoterId(UUID adrId, UUID workspaceId, UUID voterId);

    long countByAdrIdAndWorkspaceIdAndVoteType(UUID adrId, UUID workspaceId, VoteType voteType);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT COUNT(DISTINCT a.id) FROM adr a
            WHERE a.workspace_id = :wsId
              AND a.status = 'UNDER_REVIEW'
              AND NOT EXISTS (
                SELECT 1 FROM vote v
                JOIN users u ON v.voter_id = u.id
                WHERE v.adr_id = a.id
                  AND v.workspace_id = :wsId
                  AND u.role = 'APPROVER'
              )
            """, nativeQuery = true)
    Long countUnderReviewWithNoApproverVote(@org.springframework.data.repository.query.Param("wsId") UUID wsId);
}
