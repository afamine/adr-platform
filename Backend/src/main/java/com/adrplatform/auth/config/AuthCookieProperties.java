package com.adrplatform.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.cookies")
public class AuthCookieProperties {
    private String refreshName = "adr_refresh_token";
    private String csrfName = "adr_csrf";
    private String sameSite = "Strict";
    private boolean secure = true;
    private String domain;
}
