package com.adrplatform.auth.service;

import com.adrplatform.auth.config.AppProperties;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.TokenType;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.domain.WorkspaceMembership;
import com.adrplatform.auth.dto.LoginRequest;
import com.adrplatform.auth.dto.RefreshRequest;
import com.adrplatform.auth.dto.RegisterRequest;
import com.adrplatform.auth.exception.AccountDeactivatedException;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.repository.WorkspaceMembershipRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.JwtService;
import com.adrplatform.auth.security.TokenBlacklistService;
import com.adrplatform.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private WorkspaceMembershipRepository workspaceMembershipRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private TotpService totpService;
    @Mock
    private SecretEncryptionService secretEncryptionService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private AuditService auditService;
    @Spy
    private PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    private VerificationTokenService verificationTokenService;
    @Mock
    private MailService mailService;
    @Mock
    private AppProperties appProperties;
    @Mock
    private NotificationService notificationService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

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
    void registerShouldCreatePendingUserAndSendVerificationEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@adr.com");
        request.setPassword("Pass1234");
        request.setWorkspaceName("Default Workspace");
        request.setWorkspaceSlug("default");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("john@adr.com")
                .fullName("John Doe")
                .passwordHash("hashed")
                .role(Role.AUTHOR)
                .emailVerified(false)
                .isActive(false)
                .build();

        AppProperties.Token tokenProps = new AppProperties.Token();
        tokenProps.setEmailVerificationExpiryHours(24);

        when(userRepository.findByEmail("john@adr.com")).thenReturn(Optional.empty());
        when(workspaceRepository.findBySlug("default")).thenReturn(Optional.empty());
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);
        when(passwordEncoder.encode("Pass1234")).thenReturn("hashed");
        when(totpService.generateSecret()).thenReturn("totp-secret");
        when(secretEncryptionService.encrypt("totp-secret")).thenReturn("encrypted-secret");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(savedUser.getId(), workspace.getId())).thenReturn(Optional.empty());
        when(workspaceMembershipRepository.save(any(WorkspaceMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appProperties.getToken()).thenReturn(tokenProps);
        when(appProperties.getFrontendUrl()).thenReturn("http://localhost:4200");
        when(verificationTokenService.createToken(savedUser, TokenType.EMAIL_VERIFICATION, 24)).thenReturn("verif-token");

        var response = authService.register(request);

        assertThat(response.getMessage()).contains("check your email");
        assertThat(response.getEmail()).contains("@adr.com");
        verify(mailService).sendVerificationEmail("john@adr.com", "John Doe",
                "http://localhost:4200/verify-email?token=verif-token", 24);
    }

    @Test
    void registerShouldJoinExistingWorkspaceWhenSlugJoiningIsAllowed() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@adr.com");
        request.setPassword("Pass1234");
        request.setWorkspaceMode("JOIN_TEAM");
        request.setWorkspaceSlug("team-alpha");

        Workspace teamWorkspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name("Team Alpha")
                .slug("team-alpha")
                .joinPolicy("ALLOW_SLUG")
                .build();

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .workspace(teamWorkspace)
                .email("jane@adr.com")
                .fullName("Jane Doe")
                .passwordHash("hashed")
                .role(Role.AUTHOR)
                .emailVerified(false)
                .isActive(false)
                .build();

        AppProperties.Token tokenProps = new AppProperties.Token();
        tokenProps.setEmailVerificationExpiryHours(24);

        when(userRepository.findByEmail("jane@adr.com")).thenReturn(Optional.empty());
        when(workspaceRepository.findBySlug("team-alpha")).thenReturn(Optional.of(teamWorkspace));
        when(passwordEncoder.encode("Pass1234")).thenReturn("hashed");
        when(totpService.generateSecret()).thenReturn("totp-secret");
        when(secretEncryptionService.encrypt("totp-secret")).thenReturn("encrypted-secret");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(workspaceMembershipRepository.findByUser_IdAndWorkspace_Id(savedUser.getId(), teamWorkspace.getId())).thenReturn(Optional.empty());
        when(workspaceMembershipRepository.save(any(WorkspaceMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(appProperties.getToken()).thenReturn(tokenProps);
        when(appProperties.getFrontendUrl()).thenReturn("http://localhost:4200");
        when(verificationTokenService.createToken(savedUser, TokenType.EMAIL_VERIFICATION, 24)).thenReturn("verif-token");

        var response = authService.register(request);

        assertThat(response.getWorkspaceName()).isEqualTo("Team Alpha");
        assertThat(response.getWorkspaceSlug()).isEqualTo("team-alpha");
        verify(workspaceRepository, never()).save(any(Workspace.class));
        verify(mailService).sendVerificationEmail("jane@adr.com", "Jane Doe",
                "http://localhost:4200/verify-email?token=verif-token", 24);
    }

    @Test
    void registerShouldRejectSlugJoinWhenWorkspaceRequiresInvite() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@adr.com");
        request.setPassword("Pass1234");
        request.setWorkspaceMode("JOIN_TEAM");
        request.setWorkspaceSlug("team-alpha");

        Workspace teamWorkspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name("Team Alpha")
                .slug("team-alpha")
                .joinPolicy("INVITE_ONLY")
                .build();

        when(userRepository.findByEmail("jane@adr.com")).thenReturn(Optional.empty());
        when(workspaceRepository.findBySlug("team-alpha")).thenReturn(Optional.of(teamWorkspace));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("requires an invitation");
        verify(userRepository, never()).save(any(User.class));
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
                .emailVerified(true)
                .isActive(true)
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
    void loginShouldRejectDisabledVerifiedAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("disabled@adr.com");
        request.setPassword("Pass1234");

        User user = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("disabled@adr.com")
                .fullName("Disabled User")
                .passwordHash("hashed")
                .role(Role.AUTHOR)
                .emailVerified(true)
                .isActive(false)
                .build();

        when(userRepository.findByEmail("disabled@adr.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AccountDeactivatedException.class)
                .hasMessageContaining("disabled");

        verify(authenticationManager, never()).authenticate(any());
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
                .isActive(true)
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

    @Test
    void refreshShouldRejectDeactivatedUserAndRevokeRefreshTokens() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("old-refresh");

        User user = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("disabled@adr.com")
                .fullName("Disabled User")
                .passwordHash("hashed")
                .role(Role.AUTHOR)
                .isActive(false)
                .build();

        var storedToken = com.adrplatform.auth.domain.RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("old-refresh")
                .build();

        when(jwtService.isValidToken("old-refresh")).thenReturn(true);
        when(jwtService.extractTokenType("old-refresh")).thenReturn("refresh");
        when(refreshTokenService.validateRefreshToken("old-refresh")).thenReturn(storedToken);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(AccountDeactivatedException.class)
                .hasMessageContaining("disabled");

        verify(refreshTokenService).revokeAllForUser(user);
        verify(jwtService, never()).generateAccessToken(user);
        verify(jwtService, never()).generateRefreshToken(user);
    }
}
