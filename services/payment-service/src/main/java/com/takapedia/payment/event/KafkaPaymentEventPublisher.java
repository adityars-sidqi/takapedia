package com.takapedia.payment.event;

import com.takapedia.payment.exception.EventPublishingException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    static final String TOPIC = "payment.completed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaPaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(PaymentCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, event.orderId().toString(), payload);
        } catch (JacksonException e) {
            throw new EventPublishingException("Gagal serialisasi PaymentCompletedEvent", e);
        }
    }
}