package com.ecommerce.security;

import com.ecommerce.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A simple example test showing how to verify the JWT logic works correctly.
 * Run with: mvn test
 */
@SpringBootTest
class JwtTokenTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createdAccessTokenIsValidAndContainsCorrectClaims() {
        String token = jwtTokenProvider.createAccessToken("alice@tenant1.com", "CUSTOMER", "tenant1");

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("alice@tenant1.com", jwtTokenProvider.getEmail(token));
        assertEquals("CUSTOMER", jwtTokenProvider.getRole(token));
        assertEquals("tenant1", jwtTokenProvider.getTenantId(token));
        assertEquals("access", jwtTokenProvider.getTokenType(token));
    }

    @Test
    void garbageTokenIsInvalid() {
        assertFalse(jwtTokenProvider.validateToken("this-is-not-a-real-token"));
    }
}
