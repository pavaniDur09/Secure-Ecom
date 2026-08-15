package com.ecommerce.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * What we send back for /api/users/me and /api/users. Deliberately does NOT
 * include the password field - never send password hashes back to the client,
 * even hashed ones.
 */
@Data
@AllArgsConstructor
public class UserProfile {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String tenantId;
    private Set<String> permissions; // e.g. ["PRODUCT_READ", "ORDER_CREATE", ...]
}
