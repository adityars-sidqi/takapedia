package com.takapedia.auth.service;

import com.takapedia.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final long EXPIRATION_MS = 900_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService(
                new ClassPathResource("keys/private_key.pem"),
                new ClassPathResource("keys/public_key.pem"),
                EXPIRATION_MS
        );
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
        assertThat(jwtService.extractUserId(token))
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
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
    void shouldRejectTokenSignedWithDifferentKey() throws Exception {
        JwtService attackerService = new JwtService(
                new ClassPathResource("keys/attacker_private_key.pem"),
                new ClassPathResource("keys/attacker_public_key.pem"),
                EXPIRATION_MS
        );
        String forgedToken = attackerService.generateToken(sampleUser());

        assertThat(jwtService.isTokenValid(forgedToken)).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        JwtService shortLivedService = new JwtService(
                new ClassPathResource("keys/private_key.pem"),
                new ClassPathResource("keys/public_key.pem"),
                -1000L
        );
        String expiredToken = shortLivedService.generateToken(sampleUser());

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThat(jwtService.isTokenValid("ini.bukan.token")).isFalse();
        assertThat(jwtService.isTokenValid("sampah")).isFalse();
    }
}