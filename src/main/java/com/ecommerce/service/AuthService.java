package com.ecommerce.service;

import com.ecommerce.model.dto.JwtResponse;
import com.ecommerce.model.dto.LoginRequest;
import com.ecommerce.model.dto.RegisterRequest;
import com.ecommerce.model.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.audit.AuditService;
import com.ecommerce.security.jwt.JwtTokenProvider;
import com.ecommerce.security.jwt.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditService auditService;

    /** Creates a brand-new user with a hashed password. */
    public JwtResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // NEVER store raw passwords
                .role(request.getRole())
                .tenantId(request.getTenantId())
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(user);
        auditService.logSuccess("USER_REGISTERED", user.getEmail(), user.getTenantId(), "New account created");

        return buildTokens(user);
    }

    /** Verifies credentials and issues a new access + refresh token pair. */
    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditService.logFailure("AUTH_FAILED", request.getEmail(), "unknown", "No such user");
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (!user.isAccountNonLocked()) {
            auditService.logWarning("AUTH_BLOCKED", user.getEmail(), user.getTenantId(), "Account locked");
            throw new IllegalStateException("Account is locked due to too many failed login attempts");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Successful login - reset the failed-attempts counter
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        auditService.logSuccess("AUTH_SUCCESS", user.getEmail(), user.getTenantId(), "Login successful");
        return buildTokens(user);
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountNonLocked(false);
            auditService.logWarning("ACCOUNT_LOCKED", user.getEmail(), user.getTenantId(),
                    "Locked after " + attempts + " failed attempts");
        } else {
            auditService.logFailure("AUTH_FAILED", user.getEmail(), user.getTenantId(),
                    "Wrong password, attempt " + attempts + "/" + MAX_FAILED_ATTEMPTS);
        }
        userRepository.save(user);
    }

    /** Exchanges a valid refresh token for a brand-new access + refresh token pair ("rotation"). */
    public JwtResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)
                || tokenBlacklistService.isBlacklisted(refreshToken)
                || !"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));

        // Rotation: the old refresh token is now dead, a brand new pair is issued
        tokenBlacklistService.blacklist(refreshToken);
        auditService.logSuccess("TOKEN_REFRESHED", email, user.getTenantId(), "Refresh token rotated");

        return buildTokens(user);
    }

    /** Invalidates both tokens so they can no longer be used, even though JWTs don't expire server-side. */
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) tokenBlacklistService.blacklist(accessToken);
        if (refreshToken != null) tokenBlacklistService.blacklist(refreshToken);
    }

    private JwtResponse buildTokens(User user) {
        String role = user.getRole().name();
        String access = jwtTokenProvider.createAccessToken(user.getEmail(), role, user.getTenantId());
        String refresh = jwtTokenProvider.createRefreshToken(user.getEmail(), role, user.getTenantId());

        return new JwtResponse(
                access,
                refresh,
                jwtTokenProvider.getAccessTokenValiditySeconds(),
                user.getEmail(),
                role,
                user.getTenantId()
        );
    }
}
