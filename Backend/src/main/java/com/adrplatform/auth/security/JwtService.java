package com.adrplatform.auth.security;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenTtlMs());
        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(commonClaims(user, "access"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenTtlMs());
        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(commonClaims(user, "refresh"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public String extractEmail(String token) {
        return parseToken(token).get("email", String.class);
    }

    public Role extractRole(String token) {
        return Role.valueOf(parseToken(token).get("role", String.class));
    }

    public UUID extractWorkspaceId(String token) {
        return UUID.fromString(parseToken(token).get("workspaceId", String.class));
    }

    public String extractTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }

    public Instant extractExpiration(String token) {
        return parseToken(token).getExpiration().toInstant();
    }

    private Map<String, Object> commonClaims(User user, String tokenType) {
        return Map.of(
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "workspaceId", user.getWorkspace().getId().toString(),
                "type", tokenType
        );
    }
}
