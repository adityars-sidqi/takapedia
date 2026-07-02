package com.takapedia.payment.event;

import com.takapedia.payment.exception.AlreadyProcessedException;
import com.takapedia.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    static final String TOPIC = "order.created";

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    @KafkaListener(topics = TOPIC)
    public void consume(String payload) {
        OrderCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        } catch (JacksonException e) {
            // JSON rusak / tak sesuai kontrak. Log dan lanjut — jangan macetkan loop.
            // (Kontrak wire format broken adalah bug yang perlu diperbaiki di publisher,
            // bukan di-retry.)
            log.error("Failed to deserialize OrderCreatedEvent, skipping. Raw: {}", payload, e);
            return;
        }

        try {
            paymentService.initializePayment(event);
            log.info("Payment initialized for orderId={}", event.orderId());
        } catch (AlreadyProcessedException e) {
            // Bukan error — hasil normal idempotency saat event duplikat.
            log.info("Event for orderId={} already processed, skipping", event.orderId());
        } catch (Exception e) {
            // Error lain (DB down, dll): log dan lanjut. Option Z.
            // Retry/DLQ ditunda ke sub-fase resilience terpisah.
            log.error("Failed to initialize payment for orderId={}", event.orderId(), e);
        }
    }
}