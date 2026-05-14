package com.adrplatform.auth.dto;

public record UpdatePreferencesRequest(
        boolean emailOnReview,
        boolean emailOnVote,
        boolean emailOnStatus,
        boolean slackEnabled,
        String slackWebhook) {
}
