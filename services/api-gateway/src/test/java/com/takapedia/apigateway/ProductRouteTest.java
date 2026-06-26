package com.takapedia.apigateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductRouteTest {

    static WireMockServer wireMock;

    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @BeforeEach
    void setupClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void registerAuthUrl(DynamicPropertyRegistry registry) {
        registry.add("services.product.url", () -> "http://localhost:" + wireMock.port());
    }

    @Test
    void shouldRouteToProductService () {
        wireMock.stubFor(get(urlEqualTo("/api/v1/products/e671f302-e9dc-4b59-a173-6b087871da27"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "    \"id\": \"513511fb-7ede-4766-9ffc-6bdfa199f0a6\",\n" +
                                "    \"name\": \"Laptop\",\n" +
                                "    \"description\": \"Gaming laptop\",\n" +
                                "    \"price\": 15000000.00,\n" +
                                "    \"createdAt\": \"2026-06-26T16:21:41.161845Z\"\n" +
                                "}")));

        webTestClient.get()
                .uri("/api/v1/products/e671f302-e9dc-4b59-a173-6b087871da27")
                .header("Content-Type", "application/json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("513511fb-7ede-4766-9ffc-6bdfa199f0a6");
    }

}
