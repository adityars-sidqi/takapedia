package com.takapedia.payment.reconciliation;

import com.takapedia.payment.config.PaymentProperties;
import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.event.PaymentCompletedEvent;
import com.takapedia.payment.event.PaymentEventPublisher;
import com.takapedia.payment.event.PaymentOutcome;
import com.takapedia.payment.repository.PaymentRepository;
import com.takapedia.payment.service.PaymentPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @Mock
    private PaymentProperties paymentProperties;

    @InjectMocks
    private PaymentReconciliationService service;

    @BeforeEach
    void setUp() {
        when(paymentProperties.reconciliation())
                .thenReturn(new PaymentProperties.Reconciliation(Duration.ofMinutes(5)));
    }

    private Payment pending(UUID orderId) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(Instant.now().minusSeconds(60)); // sudah lewat
        return p;
    }

    @Test
    void reconcile_transitionsAndPublishesExpired_whenExpirySucceeds() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByStatusAndExpiresAtBefore(eq(PaymentStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(pending(orderId)));
        when(paymentPersistenceService.expireIfStillPending(orderId)).thenReturn(true);

        service.reconcileExpiredPayments();

        ArgumentCaptor<PaymentCompletedEvent> captor =
                ArgumentCaptor.forClass(PaymentCompletedEvent.class);
        verify(paymentEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        assertThat(captor.getValue().outcome()).isEqualTo(PaymentOutcome.EXPIRED);
    }

    @Test
    void reconcile_skipsPublish_whenWebhookWonRace() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByStatusAndExpiresAtBefore(eq(PaymentStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(pending(orderId)));
        when(paymentPersistenceService.expireIfStillPending(orderId)).thenReturn(false);

        service.reconcileExpiredPayments();

        verify(paymentEventPublisher, never()).publish(any());
    }

    @Test
    void reconcile_doesNothing_whenNoExpiredCandidates() {
        when(paymentRepository.findByStatusAndExpiresAtBefore(eq(PaymentStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of());

        service.reconcileExpiredPayments();

        verify(paymentPersistenceService, never()).expireIfStillPending(any());
        verify(paymentEventPublisher, never()).publish(any());
    }
}
