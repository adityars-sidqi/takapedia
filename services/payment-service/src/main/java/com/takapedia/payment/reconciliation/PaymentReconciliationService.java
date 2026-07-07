package com.takapedia.payment.reconciliation;

import com.takapedia.payment.config.PaymentProperties;
import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.event.PaymentCompletedEvent;
import com.takapedia.payment.event.PaymentEventPublisher;
import com.takapedia.payment.event.PaymentOutcome;
import com.takapedia.payment.repository.PaymentRepository;
import com.takapedia.payment.service.FailureReasons;
import com.takapedia.payment.service.PaymentPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reconciliation job (Fase 5c): menandai Payment PENDING yang VA-nya kedaluwarsa.
 *
 * Untuk setiap kandidat, transisi PENDING → EXPIRED dilakukan lewat conditional update
 * atomik (expireIfStillPending). Kalau webhook settlement menang balapan di detik terakhir,
 * update mengembalikan false → job TIDAK publish EXPIRED. Ini menutup race
 * "webhook vs scheduled reconciliation".
 *
 * Publish PaymentCompleted dilakukan DI LUAR transaksi DB, konsisten dengan pola Fase 5a.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final PaymentPersistenceService paymentPersistenceService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentProperties paymentProperties;

    @Scheduled(fixedDelayString = "${payment.reconciliation.scan-rate}")
    public void reconcileExpiredPayments() {
        Instant now = Instant.now();
        // Grace buffer: hanya scan yang sudah lewat expiry LEBIH dari grace,
        // supaya job (jaring pengaman) tak pernah mendahului webhook expire Midtrans.
        Instant cutoff = now.minus(paymentProperties.reconciliation().grace());
        List<Payment> candidates =
                paymentRepository.findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, cutoff);

        if (candidates.isEmpty()) {
            return;
        }
        log.info("Reconciliation: {} PENDING payment(s) melewati batas expiry", candidates.size());

        for (Payment candidate : candidates) {
            UUID orderId = candidate.getOrderId();
            boolean expired = paymentPersistenceService.expireIfStillPending(orderId);
            if (expired) {
                publishExpired(orderId, now);
                log.info("Reconciliation: orderId={} PENDING → EXPIRED", orderId);
            } else {
                log.info("Reconciliation: orderId={} sudah bukan PENDING (webhook menang race), skip",
                        orderId);
            }
        }
    }

    private void publishExpired(UUID orderId, Instant when) {
        paymentEventPublisher.publish(new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                PaymentOutcome.EXPIRED,
                null,
                FailureReasons.VA_EXPIRED,
                when));
    }
}
