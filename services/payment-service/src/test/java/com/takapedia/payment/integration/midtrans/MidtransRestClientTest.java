package com.takapedia.payment.integration.midtrans;

import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.exception.MidtransClient4xxException;
import com.takapedia.payment.exception.MidtransServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MidtransRestClientTest {

    private MockRestServiceServer mockServer;
    private MidtransRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        client = new MidtransRestClient(restClient);
    }

    @Test
    void charge_sendsExpectedRequestAndParsesSuccessResponse() {
        UUID orderId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        MidtransChargeRequest request = new MidtransChargeRequest(
                orderId, BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        String responseBody = """
                {
                  "externalReference": "VA-BCA-12345678",
                  "status": "PENDING"
                }
                """;

        mockServer.expect(requestTo("/v2/charge"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.amount").value(30_000))
                .andExpect(jsonPath("$.paymentMethod").value("BCA_VA"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        MidtransChargeResponse response = client.charge(request);

        assertThat(response.externalReference()).isEqualTo("VA-BCA-12345678");
        assertThat(response.status()).isEqualTo(MidtransPaymentStatus.PENDING);
        mockServer.verify();
    }

    @Test
    void charge_wrapsHttp5xxErrorAsMidtransServerException() {
        UUID orderId = UUID.randomUUID();
        MidtransChargeRequest request = new MidtransChargeRequest(
                orderId, BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        mockServer.expect(requestTo("/v2/charge"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.charge(request))
                .isInstanceOf(MidtransServerException.class)
                .hasMessageContaining(orderId.toString());
    }

    @Test
    void charge_wrapsHttp4xxErrorAsMidtransClient4xxException() {
        UUID orderId = UUID.randomUUID();
        MidtransChargeRequest request = new MidtransChargeRequest(
                orderId, BigDecimal.valueOf(30_000), PaymentMethod.BCA_VA);

        mockServer.expect(requestTo("/v2/charge"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.charge(request))
                .isInstanceOf(MidtransClient4xxException.class)
                .hasMessageContaining(orderId.toString());
    }
}