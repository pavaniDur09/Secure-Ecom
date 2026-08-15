package com.ecommerce.security.authorization;

import com.ecommerce.model.enums.Permission;
import com.ecommerce.model.enums.UserRole;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.ecommerce.model.enums.Permission.*;

/**
 * The single place that defines "what can each role actually do".
 * Everything else (JWT authentication filter, User entity, @PreAuthorize checks)
 * reads from this map instead of hardcoding permission lists in multiple places.
 *
 * To change what a role can do, edit ONLY this file.
 */
public final class RolePermissions {

    private RolePermissions() {
        // utility class, never instantiated
    }

    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSIONS = Map.of(

            UserRole.ADMIN, EnumSet.allOf(Permission.class), // admins can do everything

            UserRole.VENDOR, EnumSet.of(
                    PRODUCT_READ, PRODUCT_CREATE, PRODUCT_UPDATE, PRODUCT_DELETE,
                    ORDER_READ_OWN,
                    USER_READ_SELF, USER_UPDATE_SELF
            ),

            UserRole.CUSTOMER, EnumSet.of(
                    PRODUCT_READ,
                    ORDER_CREATE, ORDER_READ_OWN,
                    USER_READ_SELF, USER_UPDATE_SELF
            )
    );

    public static Set<Permission> getPermissions(UserRole role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of());
    }
}
