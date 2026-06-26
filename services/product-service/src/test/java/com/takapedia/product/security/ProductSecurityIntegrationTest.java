package com.takapedia.product.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private String tokenWithRole(String role) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("user-123")
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(privateKey)
                .compact();
    }

    @Test
    void request_withValidToken_isNotRejectedByAuth() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenWithRole("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addProduct_withMalformedToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer token.tidak.valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void request_withInvalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", "bukan-uuid-valid")
                        .header("Authorization", "Bearer " + tokenWithRole("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCatalog_withoutToken_isPublic() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());   // lolos security, produk acak tak ada
    }

    @Test
    void getCatalog_withValidToken_stillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenWithRole("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void addProduct_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addProduct_withUserRole_returns403() throws Exception {
        String validBody = """
        {"name":"Laptop","description":"Gaming laptop","price":15000000.00}
        """;
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + tokenWithRole("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void addProduct_withAdminRole_passesAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + tokenWithRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());   // lolos @PreAuthorize, kena validasi body
    }
}