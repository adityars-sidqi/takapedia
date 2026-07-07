package com.takapedia.delivery.event;

import com.takapedia.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidConsumer {

    static final String TOPIC = "order.paid";

    private final ObjectMapper objectMapper;
    private final ShipmentService shipmentService;

    @KafkaListener(topics = TOPIC)
    public void consume(String payload) {
        OrderPaidEvent event;
        try {
            event = objectMapper.readValue(payload, OrderPaidEvent.class);
        } catch (JacksonException e) {
            log.error("Failed to deserialize OrderPaidEvent, skipping. Raw: {}", payload, e);
            return;
        }

        try {
            shipmentService.createFromOrderPaid(event);
        } catch (Exception e) {
            log.error("Failed to create shipment for orderId={}", event.orderId(), e);
        }
    }
}
