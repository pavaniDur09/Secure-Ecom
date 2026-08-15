package com.ecommerce.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * What the client sends to PUT /api/users/me.
 * Password fields are optional - only include them if you're changing your password.
 */
@Data
public class UpdateProfileRequest {

    private String fullName;

    private String currentPassword; // required only if newPassword is provided

    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;
}
