package com.takapedia.payment.webhook;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.event.PaymentOutcome;
import com.takapedia.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookProcessingServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private WebhookProcessingService service;

    private Payment paymentWithVa(UUID orderId, String va) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setExternalReference(va);
        return p;
    }

    @Test
    void process_transitionsToPaidWithSuccessOutcomeAndVa_onSettlement() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(paymentWithVa(orderId, "VA-BCA-12345678")));
        when(paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.PAID, null)).thenReturn(1);

        WebhookProcessingResult result = service.process(orderId, MidtransTransactionStatus.SETTLEMENT);

        assertThat(result.outcome()).isEqualTo(WebhookProcessingResult.Outcome.TRANSITIONED);
        assertThat(result.paymentOutcome()).isEqualTo(PaymentOutcome.SUCCESS);
        assertThat(result.externalReference()).isEqualTo("VA-BCA-12345678");
        assertThat(result.failureReason()).isNull();
    }

    @Test
    void process_transitionsToExpiredWithExpiredOutcome_onExpire() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(paymentWithVa(orderId, "VA-BCA-12345678")));
        when(paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.EXPIRED, "EXPIRE")).thenReturn(1);

        WebhookProcessingResult result = service.process(orderId, MidtransTransactionStatus.EXPIRE);

        assertThat(result.outcome()).isEqualTo(WebhookProcessingResult.Outcome.TRANSITIONED);
        assertThat(result.paymentOutcome()).isEqualTo(PaymentOutcome.EXPIRED);
        assertThat(result.externalReference()).isNull();  // VA tidak relevan untuk expired
        assertThat(result.failureReason()).isEqualTo("EXPIRE");
    }

    @Test
    void process_transitionsToFailedWithFailedOutcome_onDeny() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(paymentWithVa(orderId, "VA-BCA-12345678")));
        when(paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.FAILED, "DENY")).thenReturn(1);

        WebhookProcessingResult result = service.process(orderId, MidtransTransactionStatus.DENY);

        assertThat(result.outcome()).isEqualTo(WebhookProcessingResult.Outcome.TRANSITIONED);
        assertThat(result.paymentOutcome()).isEqualTo(PaymentOutcome.FAILED);
        assertThat(result.failureReason()).isEqualTo("DENY");
    }

    @Test
    void process_returnsSkipped_whenNotPending() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(paymentWithVa(orderId, "VA-BCA-12345678")));
        when(paymentRepository.updateStatusIfCurrentMatches(
                eq(orderId), eq(PaymentStatus.PENDING), any(), any())).thenReturn(0);

        WebhookProcessingResult result = service.process(orderId, MidtransTransactionStatus.SETTLEMENT);

        assertThat(result.outcome()).isEqualTo(WebhookProcessingResult.Outcome.SKIPPED);
    }

    @Test
    void process_returnsSkipped_forPendingStatus() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(paymentWithVa(orderId, "VA")));

        WebhookProcessingResult result = service.process(orderId, MidtransTransactionStatus.PENDING);

        assertThat(result.outcome()).isEqualTo(WebhookProcessingResult.Outcome.SKIPPED);
        verify(paymentRepository, never()).updateStatusIfCurrentMatches(any(), any(), any(), any());
    }

    @Test
    void process_returnsSkipped_forUnknownStatus() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Optional.of(paymentWithVa(orderId, "VA")));

        WebhookProcessingResult result = service.process(orderId, MidtransTransactionStatus.UNKNOWN);

        assertThat(result.outcome()).isEqualTo(WebhookProcessingResult.Outcome.SKIPPED);
        verify(paymentRepository, never()).updateStatusIfCurrentMatches(any(), any(), any(), any());
    }

    @Test
    void process_returnsOrderNotFound_whenPaymentMissing() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        WebhookProcessingResult result = service.process(orderId, MidtransTransactionStatus.SETTLEMENT);

        assertThat(result.outcome()).isEqualTo(WebhookProcessingResult.Outcome.ORDER_NOT_FOUND);
        verify(paymentRepository, never()).updateStatusIfCurrentMatches(any(), any(), any(), any());
    }
}