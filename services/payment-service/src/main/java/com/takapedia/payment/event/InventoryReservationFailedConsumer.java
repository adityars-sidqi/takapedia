package com.takapedia.payment.event;

import com.takapedia.payment.service.PaymentCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Consume StockReservationFailed dari Inventory → batalkan pembayaran (void VA).
 * Group berbeda dari Order (payment-service) — dua service konsumsi topik yang sama.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationFailedConsumer {

    static final String TOPIC = "inventory.reservation-failed";

    private final ObjectMapper objectMapper;
    private final PaymentCancellationService paymentCancellationService;

    @KafkaListener(topics = TOPIC)
    public void consume(String payload) {
        StockReservationFailedEvent event;
        try {
            event = objectMapper.readValue(payload, StockReservationFailedEvent.class);
        } catch (JacksonException e) {
            log.error("Failed to deserialize StockReservationFailedEvent, skipping. Raw: {}", payload, e);
            return;
        }

        try {
            paymentCancellationService.cancelForStockFailure(event.orderId());
        } catch (Exception e) {
            log.error("Failed to cancel payment for orderId={} on stock failure", event.orderId(), e);
        }
    }
}
