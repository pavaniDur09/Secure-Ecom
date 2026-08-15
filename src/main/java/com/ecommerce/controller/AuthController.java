package com.ecommerce.controller;

import com.ecommerce.model.dto.JwtResponse;
import com.ecommerce.model.dto.LoginRequest;
import com.ecommerce.model.dto.PasswordResetConfirmRequest;
import com.ecommerce.model.dto.PasswordResetRequest;
import com.ecommerce.model.dto.RefreshTokenRequest;
import com.ecommerce.model.dto.RegisterRequest;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * All endpoints here are PUBLIC (see SecurityConfig: "/api/auth/**" is permitAll).
 * This is the only place where a client can get a token without already having one.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                        @RequestBody RefreshTokenRequest request) {
        String accessToken = null;
        String header = httpRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            accessToken = header.substring(7);
        }
        authService.logout(accessToken, request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // Step 1 of the "forgot password" flow: request a reset token.
    // Always returns the same generic message, whether or not the email exists,
    // so this endpoint can't be used to check which emails are registered.
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        String token = passwordResetService.requestReset(request.getEmail());

        Map<String, Object> body = new HashMap<>();
        body.put("message", "If that email is registered, a password reset link has been sent.");

        // ⚠️ DEMO ONLY: the token is included in the response here (only when it exists)
        // purely so you can test the reset flow without a real email server. A real
        // production API must NEVER return this token in the response - only email it.
        if (token != null) {
            body.put("demoToken", token);
        }

        return ResponseEntity.ok(body);
    }

    // Step 2 of the "forgot password" flow: use the token to set a new password.
    @PostMapping("/reset-password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request.getToken(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}

