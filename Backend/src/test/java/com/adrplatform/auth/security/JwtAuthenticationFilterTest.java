package com.adrplatform.auth.security;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private UserRepository userRepository;
    private TenantContext tenantContext;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("7e3f4f17f9a94f56a2b6d676a2390ae87e3f4f17f9a94f56a2b6d676a2390ae8");
        props.setAccessTokenTtlMs(86400000);
        props.setRefreshTokenTtlMs(604800000);

        JwtService jwtService = new JwtService(props);
        userRepository = mock(UserRepository.class);
        tenantContext = new TenantContext();
        ObjectProvider<TenantContext> provider = new ObjectProvider<>() {
            @Override
            public TenantContext getObject(Object... args) {
                return tenantContext;
            }

            @Override
            public TenantContext getIfAvailable() {
                return tenantContext;
            }

            @Override
            public TenantContext getIfUnique() {
                return tenantContext;
            }

            @Override
            public TenantContext getObject() {
                return tenantContext;
            }
        };

        ObjectProvider<UserContext> userContextProvider = new ObjectProvider<>() {
            @Override
            public UserContext getObject(Object... args) {
                return new UserContext();
            }

            @Override
            public UserContext getIfAvailable() {
                return new UserContext();
            }

            @Override
            public UserContext getIfUnique() {
                return new UserContext();
            }

            @Override
            public UserContext getObject() {
                return new UserContext();
            }
        };

        filter = new JwtAuthenticationFilter(jwtService, userRepository, provider, userContextProvider, mock(TokenBlacklistService.class));
    }

    @Test
    void shouldSetTenantContextWhenTokenValid() throws Exception {
        Workspace workspace = Workspace.builder().id(UUID.randomUUID()).name("w").slug("default").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("user@adr.com")
                .passwordHash("hashed")
                .fullName("User")
                .role(Role.AUTHOR)
                .isActive(true)
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        String token = createAccessToken(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.setServletPath("/api/users/me");
        HttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(tenantContext.getWorkspaceId()).isEqualTo(workspace.getId());
    }

    @Test
    void shouldNotAuthenticateWhenUserIsDeactivated() throws Exception {
        SecurityContextHolder.clearContext();

        Workspace workspace = Workspace.builder().id(UUID.randomUUID()).name("w").slug("default").build();
        User deactivatedUser = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("deactivated@adr.com")
                .passwordHash("hashed")
                .fullName("Deactivated User")
                .role(Role.AUTHOR)
                .isActive(false)
                .build();

        when(userRepository.findById(deactivatedUser.getId())).thenReturn(Optional.of(deactivatedUser));

        String token = createAccessToken(deactivatedUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.setServletPath("/api/adrs");
        HttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private String createAccessToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor("7e3f4f17f9a94f56a2b6d676a2390ae87e3f4f17f9a94f56a2b6d676a2390ae8".getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("workspaceId", user.getWorkspace().getId().toString())
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
