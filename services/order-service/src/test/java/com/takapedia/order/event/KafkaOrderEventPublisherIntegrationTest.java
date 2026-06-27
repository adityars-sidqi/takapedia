package com.takapedia.order.event;

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
import org.springframework.kafka.test.utils.KafkaTestUtils;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "order.created")
class KafkaOrderEventPublisherIntegrationTest {

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Test
    void shouldPublishOrderCreatedEventThatCanBeConsumedFromBroker() {
        // given — sebuah consumer manual yang subscribe ke topic
        Map<String, Object> consumerProps = new HashMap<>(
                KafkaTestUtils.consumerProps(embeddedKafkaBroker, "test-group", true));
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
        consumer.subscribe(java.util.List.of("order.created"));

        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, productId, 3, occurredAt);

        // when — publish lewat adapter sungguhan
        orderEventPublisher.publish(event);

        // then — tunggu sampai consumer benar-benar menerima pesannya
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var records = consumer.poll(Duration.ofMillis(500));
            assertThat(records.count()).isGreaterThan(0);

            ConsumerRecord<String, String> record = records.iterator().next();

            // key harus orderId
            assertThat(record.key()).isEqualTo(orderId.toString());

            // value adalah JSON yang round-trip ke event yang sama
            OrderCreatedEvent received =
                    objectMapper.readValue(record.value(), OrderCreatedEvent.class);
            assertThat(received.orderId()).isEqualTo(orderId);
            assertThat(received.productId()).isEqualTo(productId);
            assertThat(received.quantity()).isEqualTo(3);
            assertThat(received.occurredAt()).isEqualTo(occurredAt);
        });

        consumer.close();
    }
}