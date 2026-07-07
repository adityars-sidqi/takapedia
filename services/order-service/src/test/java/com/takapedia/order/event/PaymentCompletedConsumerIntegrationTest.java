package com.takapedia.order.event;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.entity.PaymentMethod;
import com.takapedia.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "payment.completed")
class PaymentCompletedConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private OrderRepository orderRepository;

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldTransitionOrderToPaidOnSuccessEvent() throws Exception {
        // Given: order WAITING_PAYMENT
        Order order = new Order();
        order.setProductId(UUID.randomUUID());
        order.setQuantity(3);
        order.setPaymentMethod(PaymentMethod.BCA_VA);
        order.setAmount(BigDecimal.valueOf(30_000));
        order.setShippingAddress("Jl. Mawar No. 1");
        order.setStatus(OrderStatus.WAITING_PAYMENT);
        Order savedOrder = orderRepository.save(order);

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                PaymentOutcome.SUCCESS,
                "VA-BCA-TEST",
                null,
                Instant.parse("2026-07-04T10:00:00Z"));

        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("payment.completed", savedOrder.getId().toString(), payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<Order> updated = orderRepository.findById(savedOrder.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getStatus()).isEqualTo(OrderStatus.PAID);
        });
    }

    @Test
    void shouldTransitionOrderToPaymentFailedOnFailedEvent() throws Exception {
        Order order = new Order();
        order.setProductId(UUID.randomUUID());
        order.setQuantity(3);
        order.setPaymentMethod(PaymentMethod.BCA_VA);
        order.setAmount(BigDecimal.valueOf(30_000));
        order.setShippingAddress("Jl. Mawar No. 1");
        order.setStatus(OrderStatus.WAITING_PAYMENT);
        Order savedOrder = orderRepository.save(order);

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                PaymentOutcome.FAILED,
                null,
                "GATEWAY_UNAVAILABLE",
                Instant.parse("2026-07-04T10:00:00Z"));

        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("payment.completed", savedOrder.getId().toString(), payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<Order> updated = orderRepository.findById(savedOrder.getId());
            assertThat(updated).isPresent();
            assertThat(updated.get().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        });
    }
}