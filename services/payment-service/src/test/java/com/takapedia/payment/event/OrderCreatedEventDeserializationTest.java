package com.takapedia.payment.event;

import com.takapedia.payment.entity.PaymentMethod;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCreatedEventDeserializationTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldDeserializeJsonFromOrderServiceWireFormat() throws Exception {
        // Payload persis seperti yang di-publish Order Service (KafkaOrderEventPublisher)
        String json = """
                {
                  "orderId": "01234567-89ab-cdef-0123-456789abcdef",
                  "productId": "fedcba98-7654-3210-fedc-ba9876543210",
                  "quantity": 3,
                  "paymentMethod": "BCA_VA",
                  "amount": 30000,
                  "occurredAt": "2026-07-02T10:00:00Z"
                }
                """;

        OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);

        assertThat(event.orderId()).isEqualTo(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"));
        assertThat(event.productId()).isEqualTo(UUID.fromString("fedcba98-7654-3210-fedc-ba9876543210"));
        assertThat(event.quantity()).isEqualTo(3);
        assertThat(event.paymentMethod()).isEqualTo(PaymentMethod.BCA_VA);
        assertThat(event.amount()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-07-02T10:00:00Z"));
    }
}