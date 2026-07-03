package com.takapedia.payment.integration.midtrans;

import com.takapedia.payment.entity.PaymentMethod;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MidtransDtoSerializationTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldSerializeChargeRequestToExpectedJsonFormat() throws Exception {
        UUID orderId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        MidtransChargeRequest request = new MidtransChargeRequest(
                orderId,
                BigDecimal.valueOf(30_000),
                PaymentMethod.BCA_VA
        );

        String json = objectMapper.writeValueAsString(request);

        // Verifikasi field ada dan format-nya sesuai
        assertThat(json).contains("\"orderId\":\"01234567-89ab-cdef-0123-456789abcdef\"");
        assertThat(json).contains("\"amount\":30000");
        assertThat(json).contains("\"paymentMethod\":\"BCA_VA\"");
    }

    @Test
    void shouldDeserializeChargeResponseFromExpectedJsonFormat() throws Exception {
        String json = """
                {
                  "externalReference": "VA-BCA-12345678",
                  "status": "PENDING"
                }
                """;

        MidtransChargeResponse response = objectMapper.readValue(json, MidtransChargeResponse.class);

        assertThat(response.externalReference()).isEqualTo("VA-BCA-12345678");
        assertThat(response.status()).isEqualTo(MidtransPaymentStatus.PENDING);
    }
}