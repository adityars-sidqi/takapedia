package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.entity.ProcessedEvent;
import com.takapedia.payment.event.OrderCreatedEvent;
import com.takapedia.payment.exception.AlreadyProcessedException;
import com.takapedia.payment.repository.PaymentRepository;
import com.takapedia.payment.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentPersistenceService {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Transaction 1: catat event sebagai processed + save Payment dengan status INITIATED.
     * Fast, in-memory (tidak ada network call). Koneksi DB dilepas segera setelah commit.
     */
    @Transactional
    public Payment initializePaymentRecord(OrderCreatedEvent event) {
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setOrderId(event.orderId());
        processedEvent.setProcessedAt(event.occurredAt());

        try {
            processedEventRepository.saveAndFlush(processedEvent);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyProcessedException(event.orderId());
        }

        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setAmount(event.amount());
        payment.setPaymentMethod(event.paymentMethod());
        payment.setStatus(PaymentStatus.INITIATED);

        return paymentRepository.save(payment);
    }

    /**
     * Transaction 2 (varian sukses): update Payment status ke SUCCESS + externalReference.
     */
    @Transactional
    public Payment finalizeSuccessfulPayment(UUID orderId, String externalReference) {
        Payment payment = findByOrderIdOrThrow(orderId);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setExternalReference(externalReference);
        return paymentRepository.save(payment);
    }

    /**
     * Transaction 2 (varian gagal): update Payment status ke FAILED + failureReason.
     */
    @Transactional
    public Payment finalizeFailedPayment(UUID orderId, String failureReason) {
        Payment payment = findByOrderIdOrThrow(orderId);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        return paymentRepository.save(payment);
    }

    private Payment findByOrderIdOrThrow(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found for orderId=" + orderId +
                                " (this should be impossible after initializePaymentRecord)"));
    }
}