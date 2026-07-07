package com.takapedia.payment.outbox;

import com.takapedia.payment.entity.OutboxEvent;
import com.takapedia.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Relay outbox: secara berkala mengirim baris outbox yang belum ter-publish ke Kafka,
 * lalu menandai publishedAt. Kalau Kafka down, baris tetap tersimpan dan dicoba lagi
 * di poll berikutnya (at-least-once; consumer sudah idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${payment.outbox.relay-rate}")
    public void relay() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pending) {
            try {
                // Blokir sampai broker konfirmasi: publishedAt hanya di-set kalau benar terkirim.
                kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()).get();
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                // Gagal kirim → biarkan tetap unpublished, hentikan batch (jaga urutan), coba lagi nanti.
                log.error("Gagal relay outbox {} ke Kafka; dicoba lagi di poll berikutnya",
                        event.getId(), e);
                return;
            }
        }
    }
}
