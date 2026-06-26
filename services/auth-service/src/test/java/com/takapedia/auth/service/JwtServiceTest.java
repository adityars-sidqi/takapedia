package com.takapedia.auth.service;

import com.takapedia.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTest {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    private User sampleUser() {
        User user = new User();
        user.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        user.setEmail("adit@takapedia.com");
        user.setRole("USER");
        return user;
    }

    @Test
    void shouldGenerateTokenContainingUserId() {
        String token = jwtService.generateToken(sampleUser());

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUserId(token)).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void shouldGenerateUniqueJtiForEachToken() {
        String token1 = jwtService.generateToken(sampleUser());
        String token2 = jwtService.generateToken(sampleUser());

        assertThat(jwtService.extractJti(token1))
                .isNotBlank()
                .isNotEqualTo(jwtService.extractJti(token2));
    }

    @Test
    void shouldValidateAFreshlyGeneratedToken() {
        String token = jwtService.generateToken(sampleUser());

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService(
                "secret-lain-yang-juga-minimal-32-byte-untuk-HS256-aman", expirationMs);
        String forgedToken = otherService.generateToken(sampleUser());

        assertThat(jwtService.isTokenValid(forgedToken)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService shortLivedService = new JwtService(secret, -1000L); // sudah lewat 1 detik
        String expiredToken = shortLivedService.generateToken(sampleUser());

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThat(jwtService.isTokenValid("ini.bukan.token")).isFalse();
        assertThat(jwtService.isTokenValid("sampah")).isFalse();
    }
}