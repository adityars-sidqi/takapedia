package com.takapedia.product.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private PrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream in = new ClassPathResource("keys/test_private_key.pem").getInputStream()) {
            privateKey = RsaKeyConverters.pkcs8().convert(in);
        }
    }

    private String validToken() {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("user-123")
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(privateKey)
                .compact();
    }

    @Test
    void request_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_withValidToken_isNotRejectedByAuth() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void request_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer token.tidak.valid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_withInvalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", "bukan-uuid-valid")
                        .header("Authorization", "Bearer " + validToken()))
                .andExpect(status().isBadRequest());
    }
}