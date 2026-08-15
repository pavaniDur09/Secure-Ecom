package com.ecommerce.model.enums;

/**
 * Fine-grained permissions, separate from roles.
 *
 * Roles (ADMIN/VENDOR/CUSTOMER) answer "who is this user?" - permissions answer
 * "exactly what are they allowed to do?" Each role maps to a SET of permissions
 * (see RolePermissions.java). This lets @PreAuthorize checks be written against a
 * specific capability (e.g. "can manage user roles") instead of a role name,
 * which is more flexible if you ever need a role's abilities to change without
 * touching every @PreAuthorize annotation in the codebase.
 */
public enum Permission {
    PRODUCT_READ,
    PRODUCT_CREATE,
    PRODUCT_UPDATE,
    PRODUCT_DELETE,

    ORDER_CREATE,
    ORDER_READ_OWN,
    ORDER_READ_ALL,

    USER_READ_SELF,
    USER_UPDATE_SELF,
    USER_READ_ALL,
    USER_MANAGE_ROLES,

    TENANT_MANAGE
}
