package com.adrplatform.auth.service;

import com.adrplatform.auth.config.PasswordResetProperties;
import com.adrplatform.auth.domain.PasswordResetToken;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.dto.ResetPasswordRequest;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.repository.PasswordResetTokenRepository;
import com.adrplatform.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;
    @Mock
    private PasswordResetProperties passwordResetProperties;
    @Mock
    private MailService mailService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        Workspace workspace = Workspace.builder()
                .id(UUID.randomUUID())
                .name("Default")
                .slug("default")
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .email("user@adr.com")
                .fullName("User")
                .passwordHash("old-hash")
                .build();

        token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("abcdef123456")
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndRevokeAllRefreshTokens() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("NewPass1234");

        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass1234")).thenReturn("new-encoded-hash");

        passwordResetService.resetPassword(request);

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-hash");
        assertThat(token.isUsed()).isTrue();
        verify(passwordResetTokenRepository).save(token);
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(user);
        verify(passwordResetTokenRepository).deleteAllByUser_Id(user.getId());
    }

    @Test
    void resetPasswordShouldRejectUsedToken() {
        token.setUsed(true);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("NewPass1234");

        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void resetPasswordShouldRejectExpiredToken() {
        token.setExpiresAt(Instant.now().minusSeconds(60));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("raw-token");
        request.setNewPassword("NewPass1234");

        when(passwordResetTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }
}
