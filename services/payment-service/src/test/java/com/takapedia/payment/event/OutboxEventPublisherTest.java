package com.takapedia.payment.event;

import com.takapedia.payment.entity.OutboxEvent;
import com.takapedia.payment.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    private OutboxEventPublisher publisher;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void setUp() {
        publisher = new OutboxEventPublisher(outboxRepository, objectMapper);
    }

    @Test
    void publish_writesOutboxRow_withTopicKeyAndPayload() {
        UUID orderId = UUID.randomUUID();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), orderId, PaymentOutcome.SUCCESS,
                "VA-BCA-12345678", null, Instant.parse("2026-07-04T10:00:00Z"));

        publisher.publish(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent row = captor.getValue();
        assertThat(row.getTopic()).isEqualTo("payment.completed");
        assertThat(row.getEventKey()).isEqualTo(orderId.toString());
        assertThat(row.getPayload()).contains(orderId.toString());
        assertThat(row.getPublishedAt()).isNull();
    }
}
