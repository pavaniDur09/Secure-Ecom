package com.ecommerce.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/** What we send back to the client after a successful login/register/refresh. */
@Data
@NoArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn; // seconds until the access token expires
    private String email;
    private String role;
    private String tenantId;

    public JwtResponse(String accessToken, String refreshToken, long expiresIn,
                        String email, String role, String tenantId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
    }
}
