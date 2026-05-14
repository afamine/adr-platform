package com.adrplatform.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record NotificationPreferencesDto(
        boolean emailOnReview,
        boolean emailOnVote,
        boolean emailOnStatus,
        boolean slackEnabled,
        String slackWebhook) {
}
