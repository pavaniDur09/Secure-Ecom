package com.ecommerce.model.enums;

/**
 * The three roles from the project spec.
 * Spring Security expects role names to start with "ROLE_" internally,
 * but we keep the enum clean and add the prefix only where Spring needs it.
 */
public enum UserRole {
    ADMIN,
    VENDOR,
    CUSTOMER
}
