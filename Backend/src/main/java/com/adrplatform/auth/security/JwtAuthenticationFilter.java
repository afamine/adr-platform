package com.adrplatform.auth.security;

import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.WorkspaceMembership;
import com.adrplatform.auth.domain.WorkspaceMembershipStatus;
import com.adrplatform.auth.repository.WorkspaceMembershipRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ObjectProvider<TenantContext> tenantContextProvider;
    private final ObjectProvider<UserContext> userContextProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (tokenBlacklistService.isBlacklisted(token)) {
            writeUnauthorized(response, "TOKEN_REVOKED", "Your session is no longer valid.");
            return;
        }

        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (ExpiredJwtException ex) {
            writeUnauthorized(response, "TOKEN_EXPIRED", "Your access token has expired.");
            return;
        } catch (Exception ex) {
            writeUnauthorized(response, "TOKEN_INVALID", "Your access token is invalid.");
            return;
        }
        if (jwtService.isPending2faToken(claims)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Pending 2FA verification required\"}");
            return;
        }
        if (!"access".equals(claims.get("type", String.class))) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UUID workspaceId = UUID.fromString(claims.get("workspaceId", String.class));
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getAuthInvalidBefore() != null
                && claims.getIssuedAt().toInstant().isBefore(user.getAuthInvalidBefore())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (user == null || !user.isActive()) {
            filterChain.doFilter(request, response);
            return;
        }
        WorkspaceMembership membership = workspaceMembershipRepository
                .findByUser_IdAndWorkspace_Id(user.getId(), workspaceId)
                .filter(item -> item.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                .orElse(null);
        if (membership == null) {
            filterChain.doFilter(request, response);
            return;
        }

        user.setWorkspace(membership.getWorkspace());
        user.setRole(membership.getRole());
        if (user.isTotpSetupRequired() && !user.isTotpEnabled()
                && !isTotpEnrollmentOrLogoutEndpoint(request.getServletPath())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"errorType\":\"TOTP_SETUP_REQUIRED\",\"message\":\"Two-factor authentication setup is required.\"}");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        tenantContextProvider.getObject().setWorkspaceId(workspaceId);
        userContextProvider.getObject().set(user);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String errorType, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"errorType\":\"" + errorType + "\",\"message\":\"" + message + "\"}");
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
    private boolean isTotpEnrollmentOrLogoutEndpoint(String path) {
        return path.equals("/api/auth/2fa/status")
                || path.equals("/api/auth/2fa/setup")
                || path.equals("/api/auth/2fa/enable")
                || path.equals("/api/auth/2fa/verify")
                || path.equals("/api/auth/logout")
                || path.equals("/api/auth/logout-all");
    }

}
