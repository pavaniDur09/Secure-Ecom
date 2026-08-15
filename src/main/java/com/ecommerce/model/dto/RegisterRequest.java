package com.ecommerce.model.dto;

import com.ecommerce.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** What the client sends to POST /api/auth/register */
@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String tenantId; // which shop this user belongs to

    private UserRole role = UserRole.CUSTOMER; // default role if not specified
}
