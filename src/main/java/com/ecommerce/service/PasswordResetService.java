package com.ecommerce.service;

import com.ecommerce.model.entity.PasswordResetToken;
import com.ecommerce.model.entity.User;
import com.ecommerce.repository.PasswordResetTokenRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles the "forgot password" flow:
 *   1) requestReset(email)   -> generates a one-time token, valid for 30 minutes
 *   2) confirmReset(token)   -> checks the token, then sets the new password
 *
 * NOTE ON EMAIL: this project has no real email server configured, so instead of
 * emailing the reset link, requestReset() logs it to the console (clearly marked
 * below) so you can copy the token and test the flow end-to-end locally. In a real
 * deployment, add spring-boot-starter-mail and actually send the link - and stop
 * returning the token in the API response, since that would let anyone reset
 * anyone else's password just by knowing their email address.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_VALIDITY_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public String requestReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        // Deliberately don't reveal whether the email exists - always behave the
        // same way from the caller's point of view, to prevent attackers from using
        // this endpoint to discover which emails are registered.
        if (user == null) {
            auditService.logWarning("PASSWORD_RESET_REQUEST", email, "unknown", "No account with this email");
            return null;
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                null, token, email, LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES), false);
        tokenRepository.save(resetToken);

        auditService.logSuccess("PASSWORD_RESET_REQUEST", email, user.getTenantId(), "Token: " + token);

        // ⚠️ DEMO ONLY: printing the token here so you can test the flow without a
        // real mail server. Remove this println once real email sending is wired up.
        System.out.println(">>> [DEMO] Password reset link for " + email
                + ": POST /api/auth/reset-password/confirm  { \"token\": \"" + token + "\", \"newPassword\": \"...\" }");

        return token;
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired");
        }

        User user = userRepository.findByEmail(resetToken.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("User no longer exists"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true); // a successful reset also un-locks a locked account
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        auditService.logSuccess("PASSWORD_RESET_COMPLETED", user.getEmail(), user.getTenantId(), "Password changed via reset token");
    }
}
