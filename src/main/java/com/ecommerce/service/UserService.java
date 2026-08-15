package com.ecommerce.service;

import com.ecommerce.model.dto.UpdateProfileRequest;
import com.ecommerce.model.dto.UserProfile;
import com.ecommerce.model.entity.User;
import com.ecommerce.model.enums.UserRole;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.audit.AuditService;
import com.ecommerce.security.authorization.RolePermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // Every logged-in user (any role) can view their own profile.
    @PreAuthorize("hasAuthority('PERMISSION_USER_READ_SELF')")
    @Transactional(readOnly = true)
    public UserProfile getMyProfile(String email) {
        return toProfile(findByEmail(email));
    }

    // Every logged-in user can update their own name/password, but not their role or tenant.
    @PreAuthorize("hasAuthority('PERMISSION_USER_UPDATE_SELF')")
    @Transactional
    public UserProfile updateMyProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        // Changing the password requires proving you know the current one -
        // this stops someone with a stolen/still-valid access token (but not the
        // password) from silently taking over the account.
        if (request.getNewPassword() != null) {
            if (request.getCurrentPassword() == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            auditService.logSuccess("PASSWORD_CHANGED", email, user.getTenantId(), "User changed their own password");
        }

        userRepository.save(user);
        return toProfile(user);
    }

    // Only admins can list every user (and only within their own tenant's data model here).
    @PreAuthorize("hasAuthority('PERMISSION_USER_READ_ALL')")
    @Transactional(readOnly = true)
    public List<UserProfile> listUsersInTenant(String tenantId) {
        return userRepository.findAll().stream()
                .filter(u -> u.getTenantId().equals(tenantId))
                .map(this::toProfile)
                .collect(Collectors.toList());
    }

    // Only admins can change someone else's role.
    @PreAuthorize("hasAuthority('PERMISSION_USER_MANAGE_ROLES')")
    @Transactional
    public UserProfile updateUserRole(Long userId, UserRole newRole, String actingAdminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserRole oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);

        auditService.logSuccess("ROLE_CHANGED", actingAdminEmail, user.getTenantId(),
                "User " + user.getEmail() + " role changed from " + oldRole + " to " + newRole);

        return toProfile(user);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private UserProfile toProfile(User user) {
        var permissionNames = RolePermissions.getPermissions(user.getRole()).stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new UserProfile(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getTenantId(),
                permissionNames
        );
    }
}
