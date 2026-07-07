package com.takapedia.auth.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.takapedia.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.*;

import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final long EXPIRATION_MS = 900_000L;

    private JwtService jwtService;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() throws Exception {
        RSAPublicKey publicKey = loadPublicKey("keys/public_key.pem");
        RSAPrivateKey privateKey = loadPrivateKey("keys/test_private_key.pem");

        jwtService = new JwtService(encoder(publicKey, privateKey), EXPIRATION_MS);
        jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private User sampleUser() {
        User user = new User();
        user.setId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        user.setEmail("adit@takapedia.com");
        user.setRole("USER");
        return user;
    }

    @Test
    void shouldGenerateTokenContainingUserIdAsSubject() {
        String token = jwtService.generateToken(sampleUser());

        assertThat(token).isNotBlank();
        Jwt decoded = jwtDecoder.decode(token);
        assertThat(decoded.getSubject())
                .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("USER");
    }

    @Test
    void shouldGenerateUniqueJtiForEachToken() {
        String token1 = jwtService.generateToken(sampleUser());
        String token2 = jwtService.generateToken(sampleUser());

        String jti1 = jwtDecoder.decode(token1).getId();
        String jti2 = jwtDecoder.decode(token2).getId();

        assertThat(jti1).isNotBlank();
        assertThat(jti1).isNotEqualTo(jti2);
    }

    @Test
    void shouldGenerateTokenVerifiableByTheMatchingDecoder() {
        String token = jwtService.generateToken(sampleUser());

        // decode() lempar exception kalau signature/expiry invalid
        assertThat(jwtDecoder.decode(token)).isNotNull();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() throws Exception {
        RSAPublicKey attackerPublic = loadPublicKey("keys/attacker_public_key.pem");
        RSAPrivateKey attackerPrivate = loadPrivateKey("keys/attacker_private_key.pem");
        JwtService attackerService = new JwtService(
                encoder(attackerPublic, attackerPrivate), EXPIRATION_MS);

        String forgedToken = attackerService.generateToken(sampleUser());

        assertThatThrownBy(() -> jwtDecoder.decode(forgedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        RSAPublicKey publicKey = loadPublicKey("keys/public_key.pem");
        RSAPrivateKey privateKey = loadPrivateKey("keys/test_private_key.pem");

        // Token dengan umur pendek (1 detik). NimbusJwtEncoder menolak expiry
        // yang mendahului issuedAt, jadi kita paksa expiry lewat lewat decoder
        // yang jam-nya dimajukan jauh melewati masa berlaku + clock skew.
        JwtService shortLivedService = new JwtService(
                encoder(publicKey, privateKey), 1000L);
        String token = shortLivedService.generateToken(sampleUser());

        NimbusJwtDecoder expiredDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(
                Clock.fixed(Instant.now().plus(1, ChronoUnit.HOURS), ZoneOffset.UTC));
        expiredDecoder.setJwtValidator(timestampValidator);

        assertThatThrownBy(() -> expiredDecoder.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> jwtDecoder.decode("ini.bukan.token"))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtDecoder.decode("sampah"))
                .isInstanceOf(JwtException.class);
    }

    private static JwtEncoder encoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("takapedia-key-1")
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    private static RSAPublicKey loadPublicKey(String path) throws Exception {
        Resource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            return (RSAPublicKey) RsaKeyConverters.x509().convert(is);
        }
    }

    private static RSAPrivateKey loadPrivateKey(String path) throws Exception {
        Resource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            return (RSAPrivateKey) RsaKeyConverters.pkcs8().convert(is);
        }
    }
}
