package com.adrplatform.auth.security;

import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.domain.WorkspaceMembershipStatus;
import com.adrplatform.auth.repository.WorkspaceMembershipRepository;
import io.jsonwebtoken.Claims;
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
        if (tokenBlacklistService.isBlacklisted(token) || !jwtService.isValidToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.parseToken(token);
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
        userRepository.findById(userId).ifPresent(user -> {
            if (!user.isActive()) {
                return;
            }
            workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(user.getId(), workspaceId)
                    .filter(membership -> membership.getStatus() == WorkspaceMembershipStatus.ACTIVE)
                    .ifPresent(membership -> {
                        user.setWorkspace(membership.getWorkspace());
                        user.setRole(membership.getRole());
                    });
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            tenantContextProvider.getObject().setWorkspaceId(workspaceId);
            userContextProvider.getObject().set(user);
        });

        filterChain.doFilter(request, response);
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
}
