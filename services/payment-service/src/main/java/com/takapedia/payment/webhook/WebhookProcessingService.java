package com.takapedia.payment.webhook;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.event.PaymentOutcome;
import com.takapedia.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Memproses webhook Midtrans: transisi status Payment secara idempotent,
 * dan mengembalikan informasi yang diperlukan untuk publish PaymentCompleted.
 *
 * Idempotency ditegakkan lewat conditional update (hanya transisi bila status
 * saat ini PENDING).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private final PaymentRepository paymentRepository;

    @CacheEvict(cacheNames = "payments", key = "#orderId")
    @Transactional
    public WebhookProcessingResult process(UUID orderId, MidtransTransactionStatus status) {
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.warn("Webhook received for unknown orderId={}", orderId);
            return WebhookProcessingResult.orderNotFound();
        }

        PaymentStatus newStatus = mapToPaymentStatus(status);
        if (newStatus == null) {
            log.info("Webhook status {} for orderId={} requires no transition", status, orderId);
            return WebhookProcessingResult.skipped();
        }

        String failureReason = (newStatus == PaymentStatus.PAID) ? null : status.name();

        int updated = paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, newStatus, failureReason);

        if (updated != 1) {
            log.info("Payment for orderId={} not in PENDING state, skipping webhook", orderId);
            return WebhookProcessingResult.skipped();
        }

        log.info("Payment for orderId={} transitioned PENDING → {}", orderId, newStatus);

        // VA number diambil dari Payment yang sudah di-fetch (tersimpan sejak charge).
        String externalReference = paymentOpt.get().getExternalReference();
        PaymentOutcome outcome = mapToOutcome(newStatus);

        return WebhookProcessingResult.transitioned(
                outcome,
                outcome == PaymentOutcome.SUCCESS ? externalReference : null,
                failureReason);
    }

    private PaymentStatus mapToPaymentStatus(MidtransTransactionStatus status) {
        return switch (status) {
            case SETTLEMENT, CAPTURE -> PaymentStatus.PAID;
            case EXPIRE -> PaymentStatus.EXPIRED;
            case DENY, CANCEL, FAILURE -> PaymentStatus.FAILED;
            case PENDING, UNKNOWN -> null;
        };
    }

    private PaymentOutcome mapToOutcome(PaymentStatus status) {
        return switch (status) {
            case PAID -> PaymentOutcome.SUCCESS;
            case EXPIRED -> PaymentOutcome.EXPIRED;
            case FAILED -> PaymentOutcome.FAILED;
            default -> throw new IllegalStateException(
                    "Unexpected status for outcome mapping: " + status);
        };
    }
}