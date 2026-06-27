package com.takapedia.apigateway.security;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.InputStream;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtFilterTest {

    static WireMockServer wireMock;

    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    PrivateKey privateKey;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void registerUrls(DynamicPropertyRegistry registry) {
        registry.add("services.product.url", () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void setup() throws Exception {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        try (InputStream in = new ClassPathResource("keys/test_private_key.pem").getInputStream()) {
            privateKey = RsaKeyConverters.pkcs8().convert(in);
        }
        wireMock.resetAll();   // bersihkan stub & rekaman antar test
        wireMock.stubFor(get(urlMatching("/api/v1/products/.*"))
                .willReturn(aResponse().withStatus(200)));
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
    void validToken_addsIdentityHeaders() {
        webTestClient.get()
                .uri("/api/v1/products/123")
                .header("Authorization", "Bearer " + tokenWithRole("USER"))
                .exchange()
                .expectStatus().isOk();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/products/123"))
                .withHeader("X-User-Id", equalTo("user-123"))
                .withHeader("X-User-Role", equalTo("USER")));
    }

    @Test
    void noToken_noIdentityHeaders() {
        webTestClient.get()
                .uri("/api/v1/products/123")
                .exchange()
                .expectStatus().isOk();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/products/123"))
                .withHeader("X-User-Id", absent()));
    }

    @Test
    void spoofedHeader_isStripped() {
        webTestClient.get()
                .uri("/api/v1/products/123")
                .header("X-User-Id", "hacker")
                .exchange()
                .expectStatus().isOk();

        wireMock.verify(getRequestedFor(urlEqualTo("/api/v1/products/123"))
                .withHeader("X-User-Id", absent()));
    }
}