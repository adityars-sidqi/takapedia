package com.takapedia.payment.webhook;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MidtransWebhookPayloadDeserializationTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldDeserializeLowercaseSettlementStatus() throws Exception {
        UUID orderId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        String json = """
                {
                  "orderId": "01234567-89ab-cdef-0123-456789abcdef",
                  "transactionStatus": "settlement",
                  "signatureKey": "abc123"
                }
                """;

        MidtransWebhookPayload payload =
                objectMapper.readValue(json, MidtransWebhookPayload.class);

        assertThat(payload.orderId()).isEqualTo(orderId);
        assertThat(payload.transactionStatus()).isEqualTo(MidtransTransactionStatus.SETTLEMENT);
        assertThat(payload.signatureKey()).isEqualTo("abc123");
    }

    @Test
    void shouldDeserializeExpireStatus() throws Exception {
        String json = """
                {
                  "orderId": "01234567-89ab-cdef-0123-456789abcdef",
                  "transactionStatus": "expire",
                  "signatureKey": "abc123"
                }
                """;

        MidtransWebhookPayload payload =
                objectMapper.readValue(json, MidtransWebhookPayload.class);

        assertThat(payload.transactionStatus()).isEqualTo(MidtransTransactionStatus.EXPIRE);
    }

    @Test
    void shouldMapUnknownStatusToUnknown() throws Exception {
        String json = """
                {
                  "orderId": "01234567-89ab-cdef-0123-456789abcdef",
                  "transactionStatus": "refund",
                  "signatureKey": "abc123"
                }
                """;

        MidtransWebhookPayload payload =
                objectMapper.readValue(json, MidtransWebhookPayload.class);

        assertThat(payload.transactionStatus()).isEqualTo(MidtransTransactionStatus.UNKNOWN);
    }
}