package com.ecommerce.security.jwt;

import com.ecommerce.model.enums.Permission;
import com.ecommerce.model.enums.UserRole;
import com.ecommerce.security.authorization.RolePermissions;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This filter runs ONCE per incoming HTTP request, BEFORE it reaches any controller.
 * Its job: look for "Authorization: Bearer <token>" header, check the token is valid,
 * and if so tell Spring Security "this request is coming from an authenticated user".
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null
                && jwtTokenProvider.validateToken(token)
                && !tokenBlacklistService.isBlacklisted(token)
                && "access".equals(jwtTokenProvider.getTokenType(token))) {

            String email = jwtTokenProvider.getEmail(token);
            String role = jwtTokenProvider.getRole(token);

            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

            // Also grant PERMISSION_* authorities so @PreAuthorize("hasAuthority('PERMISSION_X')")
            // checks work, not just hasRole(...) checks. See RolePermissions.java.
            UserRole userRole = UserRole.valueOf(role);
            for (Permission permission : RolePermissions.getPermissions(userRole)) {
                authorities.add(new SimpleGrantedAuthority("PERMISSION_" + permission.name()));
            }

            var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);

            // Make the tenantId available further down the request (e.g. in controllers/services)
            request.setAttribute("tenantId", jwtTokenProvider.getTenantId(token));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /** Pulls the raw token string out of "Authorization: Bearer xxxxx" */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
