package com.takapedia.payment.webhook;

import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Memproses webhook Midtrans: transisi status Payment secara idempotent.
 *
 * Idempotency ditegakkan lewat conditional update (hanya transisi bila status
 * saat ini PENDING). Webhook yang datang untuk Payment yang sudah final
 * (PAID/EXPIRED/FAILED) menghasilkan 0 row ter-update → di-skip diam-diam.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private final PaymentRepository paymentRepository;

    public enum ProcessResult {
        TRANSITIONED,     // status berhasil diubah dari PENDING
        SKIPPED,          // bukan PENDING (idempotent skip) atau status pending/unknown
        ORDER_NOT_FOUND   // tidak ada Payment untuk orderId ini
    }

    @Transactional
    public ProcessResult process(UUID orderId, MidtransTransactionStatus status) {
        // Cek keberadaan Payment dulu (untuk membedakan not-found vs sudah-final)
        boolean exists = paymentRepository.findByOrderId(orderId).isPresent();
        if (!exists) {
            log.warn("Webhook received for unknown orderId={}", orderId);
            return ProcessResult.ORDER_NOT_FOUND;
        }

        PaymentStatus newStatus = mapToPaymentStatus(status);
        if (newStatus == null) {
            // PENDING atau UNKNOWN dari Midtrans → tidak ada transisi
            log.info("Webhook status {} for orderId={} requires no transition",
                    status, orderId);
            return ProcessResult.SKIPPED;
        }

        String failureReason = (newStatus == PaymentStatus.PAID) ? null : status.name();

        int updated = paymentRepository.updateStatusIfCurrentMatches(
                orderId, PaymentStatus.PENDING, newStatus, failureReason);

        if (updated == 1) {
            log.info("Payment for orderId={} transitioned PENDING → {}", orderId, newStatus);
            return ProcessResult.TRANSITIONED;
        } else {
            // 0 row: status bukan PENDING (sudah diproses atau masih INITIATED)
            log.info("Payment for orderId={} not in PENDING state, skipping webhook", orderId);
            return ProcessResult.SKIPPED;
        }
    }

    /**
     * Memetakan status Midtrans ke PaymentStatus target.
     * null berarti "tidak ada transisi" (PENDING/UNKNOWN).
     */
    private PaymentStatus mapToPaymentStatus(MidtransTransactionStatus status) {
        return switch (status) {
            case SETTLEMENT, CAPTURE -> PaymentStatus.PAID;
            case EXPIRE -> PaymentStatus.EXPIRED;
            case DENY, CANCEL, FAILURE -> PaymentStatus.FAILED;
            case PENDING, UNKNOWN -> null;
        };
    }
}