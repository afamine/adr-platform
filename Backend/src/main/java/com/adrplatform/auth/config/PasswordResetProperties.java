package com.adrplatform.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "password-reset")
public class PasswordResetProperties {

    private Duration tokenTtl = Duration.ofMinutes(20);
    private String frontendUrl = "http://localhost:4200/reset-password";
    private String emailFrom = "no-reply@adrplatform.com";
    private String emailSubject = "Reset your ADR Platform password";
}
