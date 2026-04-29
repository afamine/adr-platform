package com.adrplatform.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl = "http://localhost:4200";
    private Token token = new Token();

    @Getter
    @Setter
    public static class Token {
        private int emailVerificationExpiryHours = 24;
    }
}
