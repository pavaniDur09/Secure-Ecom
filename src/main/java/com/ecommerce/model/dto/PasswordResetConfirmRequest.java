package com.ecommerce.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** What the client sends to POST /api/auth/reset-password/confirm */
@Data
public class PasswordResetConfirmRequest {
    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
