package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.exception.MidtransServerException;
import com.takapedia.payment.integration.midtrans.MidtransClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCancellationServiceTest {

    @Mock
    private PaymentPersistenceService paymentPersistenceService;

    @Mock
    private MidtransClient midtransClient;

    @InjectMocks
    private PaymentCancellationService service;

    private Payment pending(UUID orderId) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setStatus(PaymentStatus.PENDING);
        p.setExternalReference("VA-BCA-12345678");
        return p;
    }

    @Test
    void cancelForStockFailure_voidsVa_whenTransitionedFromPending() {
        UUID orderId = UUID.randomUUID();
        when(paymentPersistenceService.failIfPending(eq(orderId), any()))
                .thenReturn(Optional.of(pending(orderId)));

        service.cancelForStockFailure(orderId);

        verify(midtransClient).cancel(orderId);
    }

    @Test
    void cancelForStockFailure_skipsVoid_whenNotPending() {
        UUID orderId = UUID.randomUUID();
        when(paymentPersistenceService.failIfPending(eq(orderId), any()))
                .thenReturn(Optional.empty());

        service.cancelForStockFailure(orderId);

        verify(midtransClient, never()).cancel(any());
    }

    @Test
    void cancelForStockFailure_swallowsVoidError_soPaymentStaysFailed() {
        UUID orderId = UUID.randomUUID();
        when(paymentPersistenceService.failIfPending(eq(orderId), any()))
                .thenReturn(Optional.of(pending(orderId)));
        doThrow(new MidtransServerException("gateway down", new RuntimeException()))
                .when(midtransClient).cancel(orderId);

        // Void gagal tidak boleh mem-propagate exception — payment sudah ditandai FAILED.
        assertThatCode(() -> service.cancelForStockFailure(orderId)).doesNotThrowAnyException();
        verify(midtransClient).cancel(orderId);
    }
}
