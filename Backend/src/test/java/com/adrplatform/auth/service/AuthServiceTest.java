package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.dto.LoginRequest;
import com.adrplatform.auth.dto.RefreshRequest;
import com.adrplatform.auth.dto.RegisterRequest;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.JwtService;
import com.adrplatform.auth.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private Workspace workspace;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name("Default Workspace")
                .slug("default")
                .build();
    }

    @Test
    void registerShouldCreateUserAndTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@adr.com");
        request.setPassword("Pass1234");
        request.setWorkspaceSlug("default");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("john@adr.com")
                .fullName("John Doe")
                .passwordHash("hashed")
                .role(Role.AUTHOR)
                .build();

        when(userRepository.findByEmail("john@adr.com")).thenReturn(Optional.empty());
        when(workspaceRepository.findBySlug("default")).thenReturn(Optional.of(workspace));
        when(passwordEncoder.encode("Pass1234")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(savedUser)).thenReturn("access");
        when(jwtService.generateRefreshToken(savedUser)).thenReturn("refresh");

        var response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(response.getUser().getRole()).isEqualTo(Role.AUTHOR);
        verify(refreshTokenService).create(savedUser, "refresh");
    }

    @Test
    void registerShouldRejectWeakPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@adr.com");
        request.setPassword("weak");

        when(userRepository.findByEmail("john@adr.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password must be at least 8 chars");
    }

    @Test
    void loginShouldAuthenticateAndReturnTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@adr.com");
        request.setPassword("Pass1234");

        User user = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("john@adr.com")
                .fullName("John Doe")
                .passwordHash("hashed")
                .role(Role.AUTHOR)
                .build();

        when(userRepository.findByEmail("john@adr.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        var response = authService.login(request);

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("john@adr.com", "Pass1234"));
        assertThat(response.getToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void refreshShouldRotateRefreshToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("old-refresh");

        User user = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("john@adr.com")
                .fullName("John Doe")
                .passwordHash("hashed")
                .role(Role.AUTHOR)
                .build();

        var storedToken = com.adrplatform.auth.domain.RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("old-refresh")
                .build();

        when(jwtService.isValidToken("old-refresh")).thenReturn(true);
        when(jwtService.extractTokenType("old-refresh")).thenReturn("refresh");
        when(refreshTokenService.validateRefreshToken("old-refresh")).thenReturn(storedToken);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        var response = authService.refresh(request);

        verify(refreshTokenService).revoke(storedToken);
        verify(refreshTokenService).create(user, "new-refresh");
        assertThat(response.getToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }
}
