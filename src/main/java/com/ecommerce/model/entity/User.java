package com.ecommerce.model.entity;

import com.ecommerce.model.enums.UserRole;
import com.ecommerce.security.authorization.RolePermissions;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Our application user. It implements Spring Security's UserDetails interface,
 * which is how Spring Security knows how to check passwords/roles for this user.
 */
@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String fullName; // shown/updated via /api/users/me

    @Column(nullable = false)
    private String password; // stored as a BCrypt hash, never plain text

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // Which "shop" this user belongs to (simplified multi-tenancy, see Tenant.java)
    @Column(nullable = false)
    private String tenantId;

    @Builder.Default
    private boolean accountNonLocked = true;

    @Builder.Default
    private int failedLoginAttempts = 0;

    // ---- UserDetails interface methods Spring Security calls automatically ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Spring Security convention: role names must be prefixed with "ROLE_"
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        // Fine-grained permissions derived from the role (see RolePermissions.java).
        // Prefixed "PERMISSION_" so @PreAuthorize("hasAuthority('PERMISSION_X')")
        // can check a specific capability instead of a role name.
        RolePermissions.getPermissions(role)
                .forEach(permission -> authorities.add(new SimpleGrantedAuthority("PERMISSION_" + permission.name())));

        return authorities;
    }

    @Override
    public String getUsername() {
        return email; // we log in with email instead of a separate username field
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
