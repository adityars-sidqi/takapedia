package com.takapedia.order.event;

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
class KafkaOrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    private ObjectMapper objectMapper;
    private KafkaOrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        publisher = new KafkaOrderEventPublisher(kafkaTemplate, objectMapper);
    }

    @Test
    void shouldPublishOrderCreatedEventAsJsonToCorrectTopicWithOrderIdAsKey() throws Exception {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, productId, 3, occurredAt);

        // when
        publisher.publish(event);

        // then
        verify(kafkaTemplate).send(eq("order.created"), eq(orderId.toString()), payloadCaptor.capture());

        OrderCreatedEvent deserialized =
                objectMapper.readValue(payloadCaptor.getValue(), OrderCreatedEvent.class);
        assertThat(deserialized.orderId()).isEqualTo(orderId);
        assertThat(deserialized.productId()).isEqualTo(productId);
        assertThat(deserialized.quantity()).isEqualTo(3);
        assertThat(deserialized.occurredAt()).isEqualTo(occurredAt);
    }
}