package com.ecommerce.model.dto;

import com.ecommerce.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** What an admin sends to PUT /api/users/{id}/roles */
@Data
public class UpdateRoleRequest {
    @NotNull
    private UserRole role;
}
