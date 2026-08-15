package com.ecommerce.controller;

import com.ecommerce.model.dto.UpdateProfileRequest;
import com.ecommerce.model.dto.UpdateRoleRequest;
import com.ecommerce.model.dto.UserProfile;
import com.ecommerce.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * All endpoints here require a valid JWT. Fine-grained checks (self vs admin)
 * happen in UserService via @PreAuthorize("hasAuthority('PERMISSION_...')") -
 * this is the "permission-based" authorization the spec asks for, layered on
 * top of the role-based checks used elsewhere in the project.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfile> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(userService.updateMyProfile(authentication.getName(), request));
    }

    // Admin-only: enforced by @PreAuthorize inside UserService, not here.
    @GetMapping
    public ResponseEntity<List<UserProfile>> listUsers(HttpServletRequest httpRequest) {
        String tenantId = (String) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(userService.listUsersInTenant(tenantId));
    }

    // Admin-only: enforced by @PreAuthorize inside UserService, not here.
    @PutMapping("/{id}/roles")
    public ResponseEntity<UserProfile> updateUserRole(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateRoleRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(userService.updateUserRole(id, request.getRole(), authentication.getName()));
    }
}
