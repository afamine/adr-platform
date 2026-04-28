package com.adrplatform.auth.service;

import com.adrplatform.auth.config.AppProperties;
import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.TokenType;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.VerificationToken;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.dto.AuthResponse;
import com.adrplatform.auth.dto.LoginRequest;
import com.adrplatform.auth.dto.MessageResponse;
import com.adrplatform.auth.dto.RefreshRequest;
import com.adrplatform.auth.dto.RegisterRequest;
import com.adrplatform.auth.dto.RegisterResponse;
import com.adrplatform.auth.dto.ResendVerificationRequest;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.exception.AccountDeactivatedException;
import com.adrplatform.auth.exception.BadRequestException;
import com.adrplatform.auth.exception.EmailNotVerifiedException;
import com.adrplatform.auth.exception.ResourceNotFoundException;
import com.adrplatform.auth.exception.UnauthorizedException;
import com.adrplatform.auth.repository.UserRepository;
import com.adrplatform.auth.repository.WorkspaceRepository;
import com.adrplatform.auth.security.JwtService;
import com.adrplatform.auth.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditService auditService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final VerificationTokenService verificationTokenService;
    private final MailService mailService;
    private final AppProperties appProperties;

    /**
     * Registers a user, creates an email-verification token and sends the verification email asynchronously.
     * No JWT is issued: the user must verify their email before they can sign in.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already registered");
        }

        passwordPolicyValidator.validate(request.getPassword());

        String workspaceSlug = request.getWorkspaceSlug() == null || request.getWorkspaceSlug().isBlank()
                ? "default"
                : request.getWorkspaceSlug();

        Workspace workspace = workspaceRepository.findBySlug(workspaceSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceSlug));

        User user = User.builder()
                .workspace(workspace)
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.AUTHOR)
                .emailVerified(false)
                .isActive(false)
                .build();

        User saved = userRepository.save(user);

        int expiryHours = appProperties.getToken().getEmailVerificationExpiryHours();
        String token = verificationTokenService.createToken(saved, TokenType.EMAIL_VERIFICATION, expiryHours);
        String verificationUrl = appProperties.getFrontendUrl() + "/verify-email?token=" + token;
        mailService.sendVerificationEmail(saved.getEmail(), saved.getFullName(), verificationUrl, expiryHours);

        auditService.record(saved, workspace, "USER_REGISTERED", "USER", saved.getId(), null,
                "{\"email\":\"" + saved.getEmail() + "\",\"role\":\"" + saved.getRole().name() + "\"}");

        log.info("New user registered (pending verification): {}", saved.getEmail());
        return RegisterResponse.builder()
                .message("Account created. Please check your email to verify your account.")
                .email(maskEmail(saved.getEmail()))
                .build();
    }

    /**
     * Authenticates user credentials and returns access and refresh tokens.
     * Blocks login if the user has not verified their email address.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isActive()) {
            if (!user.isEmailVerified()) {
                throw new EmailNotVerifiedException(
                        "Please verify your email address before signing in. Check your inbox for the verification link.");
            }
            throw new AccountDeactivatedException(
                    "Your account has been deactivated. Please contact the platform administrator.");
        }

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.create(user, refreshToken);

        auditService.record(user, user.getWorkspace(), "USER_LOGGED_IN", "USER", user.getId(), null,
                "{\"email\":\"" + user.getEmail() + "\"}");

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Rotates refresh token and returns a new access token and refresh token pair.
     */
    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String oldRefreshToken = request.getRefreshToken();
        if (!jwtService.isValidToken(oldRefreshToken) || !"refresh".equals(jwtService.extractTokenType(oldRefreshToken))) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        var stored = refreshTokenService.validateRefreshToken(oldRefreshToken);
        User user = stored.getUser();

        refreshTokenService.revoke(stored);
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.create(user, newRefreshToken);

        auditService.record(user, user.getWorkspace(), "TOKEN_REFRESHED", "REFRESH_TOKEN", stored.getId(),
                "{\"token\":\"revoked\"}", "{\"token\":\"rotated\"}");

        return AuthResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(UserDto.fromEntity(user))
                .build();
    }

    /**
     * Logs out the user by blacklisting the current access token and revoking refresh tokens.
     */
    @Transactional
    public void logout(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new BadRequestException("Missing bearer token");
        }
        String accessToken = bearerToken.substring(7);

        if (!jwtService.isValidToken(accessToken)) {
            throw new UnauthorizedException("Invalid access token");
        }

        tokenBlacklistService.blacklist(accessToken);

        User user = userRepository.findById(jwtService.extractUserId(accessToken))
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        refreshTokenService.revokeAllForUser(user);

        auditService.record(user, user.getWorkspace(), "USER_LOGGED_OUT", "USER", user.getId(), null, null);
    }

    /**
     * Verifies an email-verification token, activates the user account, and returns a success message.
     * Throws TokenExpiredException / InvalidTokenException on failure (handled by GlobalExceptionHandler).
     */
    @Transactional
    public MessageResponse verifyEmail(String token) {
        VerificationToken vt = verificationTokenService.validateAndConsume(token, TokenType.EMAIL_VERIFICATION);
        User user = vt.getUser();
        user.setEmailVerified(true);
        user.setActive(true);
        userRepository.save(user);

        auditService.record(user, user.getWorkspace(), "USER_EMAIL_VERIFIED", "USER", user.getId(), null,
                "{\"email\":\"" + user.getEmail() + "\"}");

        log.info("Email verified for user {}", user.getEmail());
        return MessageResponse.builder()
                .message("Email verified successfully. You can now sign in.")
                .build();
    }

    /**
     * Re-sends a verification email. Silently succeeds for unknown emails to avoid leaking existence.
     */
    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        String normalizedEmail = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        var maybeUser = userRepository.findByEmail(normalizedEmail);
        if (maybeUser.isEmpty()) {
            log.debug("Resend verification requested for unknown email {}", normalizedEmail);
            return MessageResponse.builder()
                    .message("A new verification link has been sent to your email.")
                    .build();
        }
        User user = maybeUser.get();
        if (user.isEmailVerified()) {
            throw new BadRequestException("This email is already verified.");
        }
        int expiryHours = appProperties.getToken().getEmailVerificationExpiryHours();
        String token = verificationTokenService.createToken(user, TokenType.EMAIL_VERIFICATION, expiryHours);
        String verificationUrl = appProperties.getFrontendUrl() + "/verify-email?token=" + token;
        mailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationUrl, expiryHours);

        log.info("Verification email re-sent for user {}", user.getEmail());
        return MessageResponse.builder()
                .message("A new verification link has been sent to your email.")
                .build();
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "*".repeat(Math.max(1, local.length() - visible.length())) + domain;
    }

}
