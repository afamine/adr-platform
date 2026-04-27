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
    private String emailFrom = "adrplatform.reset@outlook.com";
    private String emailSubject = "Reset your ADR Platform password";
    private String appBaseUrl = "http://localhost:8080";
}
