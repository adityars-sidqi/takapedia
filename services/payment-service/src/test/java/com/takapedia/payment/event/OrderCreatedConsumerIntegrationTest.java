package com.takapedia.payment.event;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.entity.ProcessedEvent;
import com.takapedia.payment.integration.midtrans.MidtransChargeRequest;
import com.takapedia.payment.integration.midtrans.MidtransChargeResponse;
import com.takapedia.payment.integration.midtrans.MidtransClient;
import com.takapedia.payment.integration.midtrans.MidtransPaymentStatus;
import com.takapedia.payment.repository.PaymentRepository;
import com.takapedia.payment.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order.created", "payment.completed"})
class OrderCreatedConsumerIntegrationTest {

    @MockitoBean
    private MidtransClient midtransClient;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @BeforeEach
    void cleanDatabase() {
        paymentRepository.deleteAll();
        processedEventRepository.deleteAll();

        // Default: Midtrans return sukses
        when(midtransClient.charge(any(MidtransChargeRequest.class)))
                .thenReturn(new MidtransChargeResponse("VA-BCA-TEST", MidtransPaymentStatus.PENDING,
                        java.time.Instant.now().plusSeconds(900)));
    }

    @Test
    void shouldConsumeOrderCreatedEventAndPersistPaymentAndProcessedEvent() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-02T10:00:00Z");
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, productId, 3, PaymentMethod.BCA_VA,
                BigDecimal.valueOf(30_000), occurredAt);

        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("order.created", orderId.toString(), payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Optional<Payment> savedPayment = paymentRepository.findByOrderId(orderId);
            assertThat(savedPayment).isPresent();

            Payment payment = savedPayment.get();
            assertThat(payment.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
            assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.BCA_VA);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.getExternalReference()).isEqualTo("VA-BCA-TEST");    // ← TAMBAH
            assertThat(payment.getFailureReason()).isNull();

            Optional<ProcessedEvent> savedProcessed = processedEventRepository.findById(orderId);
            assertThat(savedProcessed).isPresent();
            assertThat(savedProcessed.get().getProcessedAt()).isEqualTo(occurredAt);
        });
    }

    @Test
    void shouldRemainIdempotent_whenSameEventIsConsumedTwice() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-02T10:00:00Z");
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, productId, 3, PaymentMethod.BCA_VA,
                BigDecimal.valueOf(30_000), occurredAt);

        String payload = objectMapper.writeValueAsString(event);

        // Publish event yang sama dua kali
        kafkaTemplate.send("order.created", orderId.toString(), payload);
        kafkaTemplate.send("order.created", orderId.toString(), payload);

        // Payment cuma satu — event kedua diabaikan karena AlreadyProcessedException
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Payment> allPayments = paymentRepository.findAll();
            assertThat(allPayments).hasSize(1);
            assertThat(allPayments.get(0).getOrderId()).isEqualTo(orderId);
        });
    }
}