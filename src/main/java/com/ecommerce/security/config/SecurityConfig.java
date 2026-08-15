package com.ecommerce.security.config;

import com.ecommerce.security.jwt.JwtAuthenticationFilter;
import com.ecommerce.security.jwt.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * This is where we tell Spring Security:
 *  1. Which URLs are public vs which require login (and which role)
 *  2. That we use JWTs instead of server-side sessions ("stateless")
 *  3. Which security HTTP headers to send back
 *  4. How passwords get hashed (BCrypt)
 *
 * Read authorizeHttpRequests(...) below top-to-bottom - rules are checked in order,
 * first match wins.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // turns on @PreAuthorize on service methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF protection is for cookie-based sessions. We use stateless JWTs
            // in an Authorization header, which aren't vulnerable to CSRF, so we disable it.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no token required
                .requestMatchers(
                    "/api/auth/**",
                    "/h2-console/**",
                    "/actuator/health"
                ).permitAll()

                // Only admins manage tenants
                .requestMatchers("/api/tenants/**").hasRole("ADMIN")

                // User profile endpoints - fine-grained admin-vs-self checks happen
                // inside UserService via @PreAuthorize("hasAuthority('PERMISSION_...')")
                .requestMatchers("/api/users/**").authenticated()

                // Product write endpoints - Admin & Vendor only (fine-grained ownership
                // check happens inside ProductService with @PreAuthorize)
                .requestMatchers("POST", "/api/products/**").hasAnyRole("ADMIN", "VENDOR")
                .requestMatchers("PUT", "/api/products/**").hasAnyRole("ADMIN", "VENDOR")
                .requestMatchers("DELETE", "/api/products/**").hasAnyRole("ADMIN", "VENDOR")

                // Anyone logged in can browse products
                .requestMatchers("GET", "/api/products/**").authenticated()

                // Orders - Admin & Customer
                .requestMatchers("/api/orders/**").hasAnyRole("ADMIN", "CUSTOMER")

                // Everything else needs at least a valid token
                .anyRequest().authenticated()
            )

            // Security headers (matches the "Sample Output" section of the spec)
            .headers(headers -> headers
                // 'unsafe-inline' is needed here because the H2 database console (a dev-only
                // tool at /h2-console) uses inline JavaScript for its login redirect.
                // For a real production API (no H2 console), tighten this back to just 'self'.
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self' 'unsafe-inline'"))
                .frameOptions(frame -> frame.sameOrigin()) // needed for the H2 console to render in an iframe
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
            )

            // Our custom filters run in this order, before Spring's own login filter:
            // 1) rate limit brute-force attempts, 2) validate the JWT
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "https://localhost:3000"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "X-Requested-With", "Accept", "X-Tenant-ID", "Origin"
        ));
        configuration.setExposedHeaders(List.of(
            "Authorization", "X-Rate-Limit-Limit", "X-Rate-Limit-Remaining"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** BCrypt hashes passwords with a "work factor" of 12 - slow on purpose, to resist cracking. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
