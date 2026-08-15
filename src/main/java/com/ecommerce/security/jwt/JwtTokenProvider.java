package com.ecommerce.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Everything JWT-related lives here: creating tokens, reading them back, and checking
 * whether a token is still valid. Think of a JWT as a signed, tamper-proof "ID card"
 * that the client carries on every request instead of a server-side session.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${security.jwt.secret}")
    private String jwtSecretBase64;

    @Value("${security.jwt.access-token-expiration-ms}")
    private long accessTokenValidityMs;

    @Value("${security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenValidityMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecretBase64);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /** A short-lived token used to authenticate normal API requests. */
    public String createAccessToken(String email, String role, String tenantId) {
        return buildToken(email, role, tenantId, "access", accessTokenValidityMs);
    }

    /** A long-lived token used ONLY to get a new access token when the old one expires. */
    public String createRefreshToken(String email, String role, String tenantId) {
        return buildToken(email, role, tenantId, "refresh", refreshTokenValidityMs);
    }

    private String buildToken(String email, String role, String tenantId, String type, long validityMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("tenantId", tenantId)
                .claim("type", type)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMs / 1000;
    }

    /** Returns true if the token's signature is valid and it hasn't expired. */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token).getBody();
    }

    public String getEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String getTenantId(String token) {
        return getClaims(token).get("tenantId", String.class);
    }

    public String getTokenType(String token) {
        return getClaims(token).get("type", String.class);
    }
}
