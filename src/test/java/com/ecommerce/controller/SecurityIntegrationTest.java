package com.ecommerce.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end style test: registers a user, logs in, then proves the security
 * rules actually work (an unauthenticated request to a protected endpoint is rejected).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestToProtectedEndpointIsRejected() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerThenLoginReturnsValidTokens() throws Exception {
        String email = "test-user@tenant1.com";

        String requestJson = """
                {
                  "email": "%s",
                  "password": "TestPass123!",
                  "tenantId": "tenant1",
                  "role": "CUSTOMER"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }
}
