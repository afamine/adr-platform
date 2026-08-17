package com.adrplatform.adr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.assist")
public record AiAssistProperties(String providerName, String privacyNotice) {
    public String providerNameOrDefault() {
        return providerName == null || providerName.isBlank() ? "external LLM provider" : providerName;
    }
    public String privacyNoticeOrDefault() {
        return privacyNotice == null || privacyNotice.isBlank()
                ? "ADR content is sent to an external AI provider for analysis. Do not include secrets or personal data."
                : privacyNotice;
    }
}
