package com.adrplatform.adr.repository;

import com.adrplatform.adr.domain.AdrComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdrCommentRepository extends JpaRepository<AdrComment, UUID> {

    List<AdrComment> findByAdrIdAndWorkspaceIdOrderByCreatedAtAsc(UUID adrId, UUID workspaceId);
}
