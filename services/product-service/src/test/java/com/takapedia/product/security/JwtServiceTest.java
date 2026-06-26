package com.takapedia.product.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.InputStream;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private PrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService(new ClassPathResource("keys/public_key.pem"));
        try (InputStream in = new ClassPathResource("keys/test_private_key.pem").getInputStream()) {
            privateKey = RsaKeyConverters.pkcs8().convert(in);
        }
    }

    private String signTokenWith(PrivateKey key, String userId, String role) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldExtractClaimsFromValidToken() {
        String token = signTokenWith(privateKey, "user-123", "USER");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThat(jwtService.isTokenValid("bukan.token.valid")).isFalse();
    }
}