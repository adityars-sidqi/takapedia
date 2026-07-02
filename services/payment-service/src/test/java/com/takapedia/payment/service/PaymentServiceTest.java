package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.entity.ProcessedEvent;
import com.takapedia.payment.event.OrderCreatedEvent;
import com.takapedia.payment.exception.AlreadyProcessedException;
import com.takapedia.payment.repository.PaymentRepository;
import com.takapedia.payment.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void initializePayment_persistsPaymentAndProcessedEvent_whenEventIsNew() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(30_000);
        Instant occurredAt = Instant.parse("2026-07-02T10:00:00Z");

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, productId, 3, PaymentMethod.BCA_VA, amount, occurredAt);

        paymentService.initializePayment(event);

        // 1. ProcessedEvent tersimpan dengan orderId & occurredAt event
        ArgumentCaptor<ProcessedEvent> processedCaptor =
                ArgumentCaptor.forClass(ProcessedEvent.class);
        verify(processedEventRepository).saveAndFlush(processedCaptor.capture());
        ProcessedEvent savedProcessed = processedCaptor.getValue();
        assertThat(savedProcessed.getOrderId()).isEqualTo(orderId);
        assertThat(savedProcessed.getProcessedAt()).isEqualTo(occurredAt);

        // 2. Payment tersimpan dengan status INITIATED dan field dari event
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getOrderId()).isEqualTo(orderId);
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(amount);
        assertThat(savedPayment.getPaymentMethod()).isEqualTo(PaymentMethod.BCA_VA);
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.INITIATED);
    }

    @Test
    void initializePayment_throwsAlreadyProcessed_andSkipsPayment_whenEventIsDuplicate() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-07-02T10:00:00Z");

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, productId, 3, PaymentMethod.BCA_VA, BigDecimal.valueOf(30_000), occurredAt);

        // Simulasikan duplicate key: insert ProcessedEvent gagal karena PK sudah ada
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));

        assertThatThrownBy(() -> paymentService.initializePayment(event))
                .isInstanceOf(AlreadyProcessedException.class)
                .hasMessageContaining(orderId.toString());

        // Payment TIDAK di-save karena exception terjadi sebelum baris paymentRepository.save
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}