package com.adrplatform.auth.security;

import com.adrplatform.auth.config.AuthCookieProperties;
import com.adrplatform.auth.dto.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthCookieService {
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private final AuthCookieProperties properties;
    private final SecureRandom random = new SecureRandom();

    public AuthResponse writeSession(HttpServletResponse response, AuthResponse authResponse, long refreshTtlMs) {
        if (authResponse.getRefreshToken() == null || authResponse.getRefreshToken().isBlank()) {
            throw new IllegalStateException("Missing refresh token for session cookie");
        }
        addCookie(response, properties.getRefreshName(), authResponse.getRefreshToken(), true,
                "/api/auth", Duration.ofMillis(refreshTtlMs));
        addCookie(response, properties.getCsrfName(), newCsrfToken(), false,
                "/api/auth", Duration.ofMillis(refreshTtlMs));
        return AuthResponse.builder()
                .token(authResponse.getToken())
                .user(authResponse.getUser())
                .requiresTwoFactor(authResponse.getRequiresTwoFactor())
                .requiresTwoFactorSetup(authResponse.getRequiresTwoFactorSetup())
                .pendingToken(authResponse.getPendingToken())
                .build();
    }

    public void clearSession(HttpServletResponse response) {
        addCookie(response, properties.getRefreshName(), "", true, "/api/auth", Duration.ZERO);
        addCookie(response, properties.getCsrfName(), "", false, "/api/auth", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String name, String value, boolean httpOnly,
                           String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(path)
                .maxAge(maxAge);
        if (properties.getDomain() != null && !properties.getDomain().isBlank()) {
            builder.domain(properties.getDomain());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private String newCsrfToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
