package com.adrplatform.adr.repository;

import com.adrplatform.adr.domain.AiAnalysisResult;
import com.adrplatform.adr.domain.AiAnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, UUID> {
    @Query("""
            select distinct result from AiAnalysisResult result
            left join fetch result.insights
            where result.adr.id = :adrId
            order by result.createdAt desc
            """)
    List<AiAnalysisResult> findLatestWithInsights(@Param("adrId") UUID adrId);

    @Query("""
            select distinct result from AiAnalysisResult result
            left join fetch result.insights
            where result.adr.id = :adrId
              and result.adrVersionHash = :adrVersionHash
              and result.status = :status
            order by result.createdAt desc
            """)
    List<AiAnalysisResult> findByAdrVersionAndStatusWithInsights(
            @Param("adrId") UUID adrId,
            @Param("adrVersionHash") String adrVersionHash,
            @Param("status") AiAnalysisStatus status);

    @Query("select distinct result from AiAnalysisResult result left join fetch result.insights where result.id = :id")
    Optional<AiAnalysisResult> findWithInsightsById(@Param("id") UUID id);
}
