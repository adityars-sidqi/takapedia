package com.takapedia.payment.integration.midtrans;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.exception.MidtransServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MidtransRestClientIntegrationTest {

    private WireMockServer wireMockServer;
    private MidtransRestClient client;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .requestFactory(factory)
                .build();

        client = new MidtransRestClient(restClient);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void charge_returnsVirtualAccountFromWireMockResponse() {
        UUID orderId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        MidtransChargeRequest request = new MidtransChargeRequest(
                orderId, BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        wireMockServer.stubFor(post(urlEqualTo("/v2/charge"))
                .withRequestBody(matchingJsonPath("$.orderId",
                        equalTo("01234567-89ab-cdef-0123-456789abcdef")))
                .withRequestBody(matchingJsonPath("$.paymentMethod", equalTo("BCA_VA")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "externalReference": "VA-BCA-12345678",
                                  "status": "PENDING"
                                }
                                """)));

        MidtransChargeResponse response = client.charge(request);

        assertThat(response.externalReference()).isEqualTo("VA-BCA-12345678");
        assertThat(response.status()).isEqualTo(MidtransPaymentStatus.PENDING);
    }

    @Test
    void charge_throwsMidtransServerException_whenGatewayReturns500() {
        UUID orderId = UUID.randomUUID();
        MidtransChargeRequest request = new MidtransChargeRequest(
                orderId, BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        wireMockServer.stubFor(post(urlEqualTo("/v2/charge"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.charge(request))
                .isInstanceOf(MidtransServerException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void charge_throwsMidtransServerException_whenGatewayResponseIsSlow() {
        UUID orderId = UUID.randomUUID();
        MidtransChargeRequest request = new MidtransChargeRequest(
                orderId, BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        // WireMock delay 5 detik, RestClient timeout 2 detik → harusnya timeout
        wireMockServer.stubFor(post(urlEqualTo("/v2/charge"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(5_000)
                        .withBody("""
                                {
                                  "externalReference": "VA-BCA-12345678",
                                  "status": "PENDING"
                                }
                                """)));

        assertThatThrownBy(() -> client.charge(request))
                .isInstanceOf(MidtransServerException.class)
                .hasMessageContaining(orderId.toString());
    }
}