package com.ecommerce.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** What the client sends to POST /api/auth/reset-password */
@Data
public class PasswordResetRequest {
    @NotBlank
    @Email
    private String email;
}
