package com.takapedia.order.event;

import com.takapedia.order.service.OrderCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockReservationFailedConsumer {

    static final String TOPIC = "inventory.reservation-failed";

    private final ObjectMapper objectMapper;
    private final OrderCancellationService orderCancellationService;

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
            orderCancellationService.cancelDueToStockFailure(event.orderId());
            log.info("Order {} cancelled due to stock reservation failure (reason={})",
                    event.orderId(), event.reason());
        } catch (Exception e) {
            log.error("Failed to cancel order {} on stock failure", event.orderId(), e);
        }
    }
}
