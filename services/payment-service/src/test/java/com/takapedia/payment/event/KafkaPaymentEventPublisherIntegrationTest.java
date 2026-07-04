package com.takapedia.payment.event;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "payment.completed")
class KafkaPaymentEventPublisherIntegrationTest {

    @Autowired
    private PaymentEventPublisher paymentEventPublisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldPublishPaymentCompletedEventThatCanBeConsumedFromBroker() {
        Map<String, Object> consumerProps = new HashMap<>(
                org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps(
                        embeddedKafkaBroker, "test-group", true));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(List.of("payment.completed"));

        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-04T10:00:00Z");
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId, orderId, PaymentOutcome.SUCCESS,
                "VA-BCA-12345678", null, occurredAt);

        paymentEventPublisher.publish(event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var records = consumer.poll(Duration.ofMillis(500));
            assertThat(records.count()).isGreaterThan(0);

            ConsumerRecord<String, String> record = records.iterator().next();
            assertThat(record.key()).isEqualTo(orderId.toString());

            PaymentCompletedEvent received =
                    objectMapper.readValue(record.value(), PaymentCompletedEvent.class);
            assertThat(received).isEqualTo(event);
        });

        consumer.close();
    }
}