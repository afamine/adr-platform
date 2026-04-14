package com.adrplatform.auth.service;

import com.adrplatform.auth.domain.Role;
import com.adrplatform.auth.domain.User;
import com.adrplatform.auth.domain.Workspace;
import com.adrplatform.auth.dto.AuthResponse;
import com.adrplatform.auth.dto.LoginRequest;
import com.adrplatform.auth.dto.RefreshRequest;
import com.adrplatform.auth.dto.RegisterRequest;
import com.adrplatform.auth.dto.UserDto;
import com.adrplatform.auth.exception.BadRequestException;
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

    /**
     * Registers a user in the resolved workspace and returns access and refresh tokens.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
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
                .build();

        User saved = userRepository.save(user);
        String accessToken = jwtService.generateAccessToken(saved);
        String refreshToken = jwtService.generateRefreshToken(saved);
        refreshTokenService.create(saved, refreshToken);

        auditService.record(saved, workspace, "USER_REGISTERED", "USER", saved.getId(), null,
                "{\"email\":\"" + saved.getEmail() + "\",\"role\":\"" + saved.getRole().name() + "\"}");

        log.info("New user registered: {}", saved.getEmail());
        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(UserDto.fromEntity(saved))
                .build();
    }

    /**
     * Authenticates user credentials and returns access and refresh tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

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

}
