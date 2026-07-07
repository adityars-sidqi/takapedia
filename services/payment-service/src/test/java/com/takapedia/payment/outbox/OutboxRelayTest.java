package com.takapedia.payment.outbox;

import com.takapedia.payment.entity.OutboxEvent;
import com.takapedia.payment.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutboxRelay relay;

    @Test
    void relay_publishesPendingAndMarksSent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setTopic("payment.completed");
        event.setEventKey("order-key");
        event.setPayload("{\"outcome\":\"SUCCESS\"}");

        when(outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send("payment.completed", "order-key", "{\"outcome\":\"SUCCESS\"}"))
                .thenReturn(future);

        relay.relay();

        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxRepository).save(event);
    }

    @Test
    void relay_doesNothing_whenNoPending() {
        when(outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of());

        relay.relay();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
