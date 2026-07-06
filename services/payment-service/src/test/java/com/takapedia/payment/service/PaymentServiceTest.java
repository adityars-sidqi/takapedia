package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.event.OrderCreatedEvent;
import com.takapedia.payment.event.PaymentCompletedEvent;
import com.takapedia.payment.event.PaymentEventPublisher;
import com.takapedia.payment.event.PaymentOutcome;
import com.takapedia.payment.exception.AlreadyProcessedException;
import com.takapedia.payment.exception.MidtransClient4xxException;
import com.takapedia.payment.exception.MidtransServerException;
import com.takapedia.payment.integration.midtrans.MidtransChargeRequest;
import com.takapedia.payment.integration.midtrans.MidtransChargeResponse;
import com.takapedia.payment.integration.midtrans.MidtransClient;
import com.takapedia.payment.integration.midtrans.MidtransPaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

    @Mock
    private MidtransClient midtransClient;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private OrderCreatedEvent sampleEvent(UUID orderId) {
        return new OrderCreatedEvent(
                orderId,
                UUID.randomUUID(),
                3,
                PaymentMethod.BCA_VA,
                BigDecimal.valueOf(30_000),
                Instant.parse("2026-07-04T10:00:00Z")
        );
    }

    private Payment initiatedPayment(UUID orderId) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrderId(orderId);
        payment.setAmount(BigDecimal.valueOf(30_000));
        payment.setPaymentMethod(PaymentMethod.BCA_VA);
        return payment;
    }

    @Test
    void initializePayment_marksPendingAndDoesNotPublish_whenChargeSucceeds() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = sampleEvent(orderId);
        Payment initiated = initiatedPayment(orderId);

        when(paymentPersistenceService.initializePaymentRecord(event)).thenReturn(initiated);
        when(midtransClient.charge(any(MidtransChargeRequest.class))).thenReturn(
                new MidtransChargeResponse("VA-BCA-12345678", MidtransPaymentStatus.PENDING));

        paymentService.initializePayment(event);

        // Charge sukses → markPending dipanggil dengan VA number
        verify(paymentPersistenceService).markPending(orderId, "VA-BCA-12345678");
        verify(paymentPersistenceService, never()).finalizeFailedPayment(any(), any());

        // TIDAK ada publish PaymentCompleted saat charge sukses
        verify(paymentEventPublisher, never()).publish(any());
    }

    @Test
    void initializePayment_finalizesFailedAndPublishesFailedEvent_whenGatewayUnavailable() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = sampleEvent(orderId);
        Payment initiated = initiatedPayment(orderId);

        when(paymentPersistenceService.initializePaymentRecord(event)).thenReturn(initiated);
        when(midtransClient.charge(any(MidtransChargeRequest.class)))
                .thenThrow(new MidtransServerException("gateway down", new RuntimeException()));

        paymentService.initializePayment(event);

        verify(paymentPersistenceService).finalizeFailedPayment(orderId, FailureReasons.GATEWAY_UNAVAILABLE);
        verify(paymentPersistenceService, never()).markPending(any(), any());

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisher).publish(eventCaptor.capture());
        PaymentCompletedEvent published = eventCaptor.getValue();
        assertThat(published.orderId()).isEqualTo(orderId);
        assertThat(published.outcome()).isEqualTo(PaymentOutcome.FAILED);
        assertThat(published.externalReference()).isNull();
        assertThat(published.failureReason()).isEqualTo(FailureReasons.GATEWAY_UNAVAILABLE);
    }

    @Test
    void initializePayment_finalizesFailedAndPublishesFailedEvent_whenGatewayRejects() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = sampleEvent(orderId);
        Payment initiated = initiatedPayment(orderId);

        when(paymentPersistenceService.initializePaymentRecord(event)).thenReturn(initiated);
        when(midtransClient.charge(any(MidtransChargeRequest.class)))
                .thenThrow(new MidtransClient4xxException("bad request", new RuntimeException()));

        paymentService.initializePayment(event);

        verify(paymentPersistenceService).finalizeFailedPayment(orderId, FailureReasons.REJECTED_BY_GATEWAY);
        verify(paymentPersistenceService, never()).markPending(any(), any());

        ArgumentCaptor<PaymentCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisher).publish(eventCaptor.capture());
        PaymentCompletedEvent published = eventCaptor.getValue();
        assertThat(published.outcome()).isEqualTo(PaymentOutcome.FAILED);
        assertThat(published.failureReason()).isEqualTo(FailureReasons.REJECTED_BY_GATEWAY);
    }

    @Test
    void initializePayment_propagatesAlreadyProcessed_andDoesNotCallMidtransOrPublisher() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = sampleEvent(orderId);

        when(paymentPersistenceService.initializePaymentRecord(event))
                .thenThrow(new AlreadyProcessedException(orderId));

        assertThatThrownBy(() -> paymentService.initializePayment(event))
                .isInstanceOf(AlreadyProcessedException.class);

        verify(midtransClient, never()).charge(any());
        verify(paymentPersistenceService, never()).markPending(any(), any());
        verify(paymentPersistenceService, never()).finalizeFailedPayment(any(), any());
        verify(paymentEventPublisher, never()).publish(any());
    }
}