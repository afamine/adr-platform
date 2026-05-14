package com.adrplatform.analytics.dto;

public record KpiDto(
        long totalAdrs,
        long totalAdrsThisMonth,
        double acceptanceRate,
        long acceptedCount,
        long rejectedCount,
        double avgReviewTimeDays,
        Double avgReviewTimeDelta,
        long pendingVotes,
        long pendingApproverDecisions) {
}
