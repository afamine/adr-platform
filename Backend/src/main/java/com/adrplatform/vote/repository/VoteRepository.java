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
}
