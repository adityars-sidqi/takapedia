package com.takapedia.order.event;

import com.takapedia.order.service.OrderPaymentUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedConsumer {

    static final String TOPIC = "payment.completed";

    private final ObjectMapper objectMapper;
    private final OrderPaymentUpdateService orderPaymentUpdateService;

    @KafkaListener(topics = TOPIC)
    public void consume(String payload) {
        PaymentCompletedEvent event;
        try {
            event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
        } catch (JacksonException e) {
            log.error("Failed to deserialize PaymentCompletedEvent, skipping. Raw: {}", payload, e);
            return;
        }

        try {
            orderPaymentUpdateService.applyPaymentOutcome(event);
            log.info("Order {} updated with payment outcome={}",
                    event.orderId(), event.outcome());
        } catch (Exception e) {
            // Option Z: log & continue, jangan macetkan loop
            log.error("Failed to apply payment outcome for orderId={}",
                    event.orderId(), e);
        }
    }
}