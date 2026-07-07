package com.takapedia.payment.event;

import com.takapedia.payment.entity.OutboxEvent;
import com.takapedia.payment.exception.EventPublishingException;
import com.takapedia.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementasi PaymentEventPublisher berbasis outbox: menulis event ke tabel outbox
 * (bukan kirim ke Kafka langsung). Karena penulisan ini terjadi tepat setelah/di dalam
 * alur transaksi Payment, event tak hilang walau Kafka sedang tak tersedia — OutboxRelay
 * yang mengurus pengiriman ke Kafka secara terpisah + retry.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventPublisher implements PaymentEventPublisher {

    static final String TOPIC = "payment.completed";

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(PaymentCompletedEvent event) {
        try {
            OutboxEvent row = new OutboxEvent();
            row.setTopic(TOPIC);
            row.setEventKey(event.orderId().toString());
            row.setPayload(objectMapper.writeValueAsString(event));
            outboxRepository.save(row);
        } catch (JacksonException e) {
            throw new EventPublishingException("Gagal serialisasi PaymentCompletedEvent ke outbox", e);
        }
    }
}
