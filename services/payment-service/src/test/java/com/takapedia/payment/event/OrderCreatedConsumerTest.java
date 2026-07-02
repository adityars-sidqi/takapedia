package com.takapedia.payment.event;

import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.exception.AlreadyProcessedException;
import com.takapedia.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private PaymentService paymentService;

    private ObjectMapper objectMapper;
    private OrderCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        consumer = new OrderCreatedConsumer(objectMapper, paymentService);
    }

    @Test
    void consume_deserializesPayloadAndDelegatesToPaymentService() {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                  "orderId": "%s",
                  "productId": "%s",
                  "quantity": 3,
                  "paymentMethod": "BCA_VA",
                  "amount": 30000,
                  "occurredAt": "2026-07-02T10:00:00Z"
                }
                """.formatted(orderId, UUID.randomUUID());

        consumer.consume(payload);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(paymentService).initializePayment(eventCaptor.capture());
        OrderCreatedEvent event = eventCaptor.getValue();

        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.paymentMethod()).isEqualTo(PaymentMethod.BCA_VA);
        assertThat(event.amount()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-07-02T10:00:00Z"));
    }

    @Test
    void consume_skipsSilently_whenPayloadIsMalformed() {
        String malformedPayload = "{not a valid json";

        // Nggak throw — consumer catch internally
        consumer.consume(malformedPayload);

        // PaymentService nggak pernah dipanggil
        verify(paymentService, never()).initializePayment(any(OrderCreatedEvent.class));
    }

    @Test
    void consume_skipsSilently_whenPaymentServiceReportsAlreadyProcessed() {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                  "orderId": "%s",
                  "productId": "%s",
                  "quantity": 3,
                  "paymentMethod": "BCA_VA",
                  "amount": 30000,
                  "occurredAt": "2026-07-02T10:00:00Z"
                }
                """.formatted(orderId, UUID.randomUUID());

        doThrow(new AlreadyProcessedException(orderId))
                .when(paymentService).initializePayment(any(OrderCreatedEvent.class));

        // Nggak throw — AlreadyProcessedException di-catch internally
        consumer.consume(payload);

        verify(paymentService).initializePayment(any(OrderCreatedEvent.class));
    }

    @Test
    void consume_skipsSilently_whenPaymentServiceThrowsUnexpected() {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                  "orderId": "%s",
                  "productId": "%s",
                  "quantity": 3,
                  "paymentMethod": "BCA_VA",
                  "amount": 30000,
                  "occurredAt": "2026-07-02T10:00:00Z"
                }
                """.formatted(orderId, UUID.randomUUID());

        doThrow(new RuntimeException("DB down"))
                .when(paymentService).initializePayment(any(OrderCreatedEvent.class));

        // Nggak throw — generic exception di-catch internally (Option Z)
        consumer.consume(payload);

        verify(paymentService).initializePayment(any(OrderCreatedEvent.class));
    }
}