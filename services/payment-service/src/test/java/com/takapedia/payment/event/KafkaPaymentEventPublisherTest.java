package com.takapedia.payment.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaPaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    private ObjectMapper objectMapper;
    private KafkaPaymentEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        publisher = new KafkaPaymentEventPublisher(kafkaTemplate, objectMapper);
    }

    @Test
    void shouldPublishPaymentCompletedSuccessEventAsJsonWithOrderIdAsKey() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-04T10:00:00Z");

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId, orderId, PaymentOutcome.SUCCESS,
                "VA-BCA-12345678", null, occurredAt);

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("payment.completed"), eq(orderId.toString()), payloadCaptor.capture());

        PaymentCompletedEvent deserialized =
                objectMapper.readValue(payloadCaptor.getValue(), PaymentCompletedEvent.class);
        assertThat(deserialized).isEqualTo(event);
    }

    @Test
    void shouldPublishPaymentCompletedFailedEventAsJsonWithOrderIdAsKey() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-04T10:00:00Z");

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId, orderId, PaymentOutcome.FAILED,
                null, "GATEWAY_UNAVAILABLE", occurredAt);

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("payment.completed"), eq(orderId.toString()), payloadCaptor.capture());

        PaymentCompletedEvent deserialized =
                objectMapper.readValue(payloadCaptor.getValue(), PaymentCompletedEvent.class);
        assertThat(deserialized).isEqualTo(event);
    }
}