package com.takapedia.payment.event;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCompletedEventDeserializationTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldRoundTripSuccessEventThroughJson() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-04T10:00:00Z");

        PaymentCompletedEvent original = new PaymentCompletedEvent(
                eventId,
                orderId,
                PaymentOutcome.SUCCESS,
                "VA-BCA-12345678",
                null,
                occurredAt
        );

        String json = objectMapper.writeValueAsString(original);
        PaymentCompletedEvent roundTripped =
                objectMapper.readValue(json, PaymentCompletedEvent.class);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void shouldRoundTripFailedEventThroughJson() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-04T10:00:00Z");

        PaymentCompletedEvent original = new PaymentCompletedEvent(
                eventId,
                orderId,
                PaymentOutcome.FAILED,
                null,
                "GATEWAY_UNAVAILABLE",
                occurredAt
        );

        String json = objectMapper.writeValueAsString(original);
        PaymentCompletedEvent roundTripped =
                objectMapper.readValue(json, PaymentCompletedEvent.class);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void shouldSerializeSuccessEventWithExpectedFieldNames() throws Exception {
        UUID orderId = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                PaymentOutcome.SUCCESS,
                "VA-BCA-12345678",
                null,
                Instant.parse("2026-07-04T10:00:00Z")
        );

        String json = objectMapper.writeValueAsString(event);

        // Verifikasi kontrak field name (yang akan di-consume Order Service)
        assertThat(json).contains("\"orderId\":\"01234567-89ab-cdef-0123-456789abcdef\"");
        assertThat(json).contains("\"outcome\":\"SUCCESS\"");
        assertThat(json).contains("\"externalReference\":\"VA-BCA-12345678\"");
        assertThat(json).contains("\"occurredAt\":\"2026-07-04T10:00:00Z\"");
    }
}