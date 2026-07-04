package com.takapedia.order.event;

import com.takapedia.order.service.OrderPaymentUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCompletedConsumerTest {

    @Mock
    private OrderPaymentUpdateService orderPaymentUpdateService;

    private ObjectMapper objectMapper;
    private PaymentCompletedConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        consumer = new PaymentCompletedConsumer(objectMapper, orderPaymentUpdateService);
    }

    @Test
    void consume_deserializesSuccessEventAndDelegatesToService() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "outcome": "SUCCESS",
                  "externalReference": "VA-BCA-12345678",
                  "failureReason": null,
                  "occurredAt": "2026-07-04T10:00:00Z"
                }
                """.formatted(eventId, orderId);

        consumer.consume(payload);

        ArgumentCaptor<PaymentCompletedEvent> captor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(orderPaymentUpdateService).applyPaymentOutcome(captor.capture());
        PaymentCompletedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.outcome()).isEqualTo(PaymentOutcome.SUCCESS);
        assertThat(event.externalReference()).isEqualTo("VA-BCA-12345678");
    }

    @Test
    void consume_deserializesFailedEventAndDelegatesToService() {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "outcome": "FAILED",
                  "externalReference": null,
                  "failureReason": "GATEWAY_UNAVAILABLE",
                  "occurredAt": "2026-07-04T10:00:00Z"
                }
                """.formatted(UUID.randomUUID(), orderId);

        consumer.consume(payload);

        ArgumentCaptor<PaymentCompletedEvent> captor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(orderPaymentUpdateService).applyPaymentOutcome(captor.capture());
        assertThat(captor.getValue().outcome()).isEqualTo(PaymentOutcome.FAILED);
        assertThat(captor.getValue().failureReason()).isEqualTo("GATEWAY_UNAVAILABLE");
    }

    @Test
    void consume_skipsSilently_whenPayloadIsMalformed() {
        consumer.consume("{not a valid json");

        verify(orderPaymentUpdateService, never()).applyPaymentOutcome(any());
    }

    @Test
    void consume_skipsSilently_whenServiceThrows() {
        UUID orderId = UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "orderId": "%s",
                  "outcome": "SUCCESS",
                  "externalReference": "VA-BCA-12345678",
                  "failureReason": null,
                  "occurredAt": "2026-07-04T10:00:00Z"
                }
                """.formatted(UUID.randomUUID(), orderId);

        doThrow(new RuntimeException("DB down"))
                .when(orderPaymentUpdateService).applyPaymentOutcome(any());

        // Nggak throw — di-catch dan log
        consumer.consume(payload);

        verify(orderPaymentUpdateService).applyPaymentOutcome(any());
    }
}