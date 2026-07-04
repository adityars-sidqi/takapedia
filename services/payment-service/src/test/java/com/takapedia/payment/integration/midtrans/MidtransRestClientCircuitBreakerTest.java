package com.takapedia.payment.integration.midtrans;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.exception.MidtransServerException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class MidtransRestClientCircuitBreakerTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MidtransRestClient client;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void midtransProperties(DynamicPropertyRegistry registry) {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        registry.add("midtrans.base-url", () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeEach
    void resetCircuitBreaker() {
        wireMockServer.resetAll();
        // Reset circuit breaker state antar test — sangat penting untuk isolasi
        circuitBreakerRegistry.circuitBreaker("midtrans").reset();
    }

    @AfterEach
    void tearDown() {
        // wireMockServer lifecycle di-manage via @DynamicPropertySource (satu instance per test class)
    }

    @Test
    void circuitBreaker_opensAfterConsecutiveFailures() {
        // Config test: slidingWindowSize=3, minimumNumberOfCalls=2, threshold=50%
        // Berarti setelah 2 call gagal, circuit langsung OPEN

        wireMockServer.stubFor(post(urlEqualTo("/v2/charge"))
                .willReturn(aResponse().withStatus(500)));

        MidtransChargeRequest request = new MidtransChargeRequest(
                UUID.randomUUID(), BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        // Call 1: gagal 5xx → MidtransServerException
        assertThatThrownBy(() -> client.charge(request))
                .isInstanceOf(MidtransServerException.class);

        // Call 2: gagal 5xx → threshold tercapai (2/2 gagal = 100%), circuit OPEN
        assertThatThrownBy(() -> client.charge(request))
                .isInstanceOf(MidtransServerException.class);

        // Verifikasi state OPEN
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("midtrans");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Call 3: circuit OPEN → fallback dipanggil TANPA hit WireMock
        // Fallback throw MidtransServerException dengan message "circuit breaker fallback"
        assertThatThrownBy(() -> client.charge(request))
                .isInstanceOf(MidtransServerException.class)
                .hasMessageContaining("circuit breaker fallback");
    }

    @Test
    void circuitBreaker_ignores4xxErrors() {
        // 4xx = MidtransClient4xxException = ignoreExceptions → CB tidak buka

        wireMockServer.stubFor(post(urlEqualTo("/v2/charge"))
                .willReturn(aResponse().withStatus(400)));

        MidtransChargeRequest request = new MidtransChargeRequest(
                UUID.randomUUID(), BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        // Beberapa call 4xx berturut-turut
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> client.charge(request))
                    .isInstanceOf(com.takapedia.payment.exception.MidtransClient4xxException.class);
        }

        // Circuit tetap CLOSED walau semua call gagal — karena 4xx di-ignore
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("midtrans");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}