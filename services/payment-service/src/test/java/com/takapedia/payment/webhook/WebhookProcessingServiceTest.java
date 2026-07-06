package com.takapedia.payment.webhook;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
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

    private Payment somePayment(UUID orderId) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        return p;
    }

    @Test
    void process_transitionsToPaid_onSettlement() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(somePayment(orderId)));
        when(paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.PAID, null)).thenReturn(1);

        var result = service.process(orderId, MidtransTransactionStatus.SETTLEMENT);

        assertThat(result).isEqualTo(WebhookProcessingService.ProcessResult.TRANSITIONED);
        verify(paymentRepository).updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.PAID, null);
    }

    @Test
    void process_transitionsToExpired_onExpire() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(somePayment(orderId)));
        when(paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.EXPIRED, "EXPIRE")).thenReturn(1);

        var result = service.process(orderId, MidtransTransactionStatus.EXPIRE);

        assertThat(result).isEqualTo(WebhookProcessingService.ProcessResult.TRANSITIONED);
        verify(paymentRepository).updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.EXPIRED, "EXPIRE");
    }

    @Test
    void process_transitionsToFailed_onDeny() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(somePayment(orderId)));
        when(paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, PaymentStatus.FAILED, "DENY")).thenReturn(1);

        var result = service.process(orderId, MidtransTransactionStatus.DENY);

        assertThat(result).isEqualTo(WebhookProcessingService.ProcessResult.TRANSITIONED);
    }

    @Test
    void process_returnsSkipped_whenNotPending() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(somePayment(orderId)));
        // Conditional update return 0 = status bukan PENDING
        when(paymentRepository.updateStatusIfCurrentMatches(
                eq(orderId), eq(PaymentStatus.PENDING), any(), any())).thenReturn(0);

        var result = service.process(orderId, MidtransTransactionStatus.SETTLEMENT);

        assertThat(result).isEqualTo(WebhookProcessingService.ProcessResult.SKIPPED);
    }

    @Test
    void process_returnsSkipped_forPendingStatus_withoutTouchingDb() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(somePayment(orderId)));

        var result = service.process(orderId, MidtransTransactionStatus.PENDING);

        assertThat(result).isEqualTo(WebhookProcessingService.ProcessResult.SKIPPED);
        verify(paymentRepository, never()).updateStatusIfCurrentMatches(any(), any(), any(), any());
    }

    @Test
    void process_returnsSkipped_forUnknownStatus() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(somePayment(orderId)));

        var result = service.process(orderId, MidtransTransactionStatus.UNKNOWN);

        assertThat(result).isEqualTo(WebhookProcessingService.ProcessResult.SKIPPED);
        verify(paymentRepository, never()).updateStatusIfCurrentMatches(any(), any(), any(), any());
    }

    @Test
    void process_returnsOrderNotFound_whenPaymentMissing() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        var result = service.process(orderId, MidtransTransactionStatus.SETTLEMENT);

        assertThat(result).isEqualTo(WebhookProcessingService.ProcessResult.ORDER_NOT_FOUND);
        verify(paymentRepository, never()).updateStatusIfCurrentMatches(any(), any(), any(), any());
    }
}