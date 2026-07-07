package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.integration.midtrans.MidtransClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Kompensasi pembayaran saat Inventory gagal reserve (stok kurang).
 *
 * Transisi PENDING → FAILED dilakukan atomik (failIfPending), aman terhadap race dengan
 * webhook. Jika berhasil, VA yang sudah dibuat di-void di Midtrans DI LUAR transaksi
 * (best-effort — kalau void gagal, payment tetap FAILED; VA sisa bisa ditangani reconciliation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCancellationService {

    private final PaymentPersistenceService paymentPersistenceService;
    private final MidtransClient midtransClient;

    public void cancelForStockFailure(UUID orderId) {
        Optional<Payment> failed =
                paymentPersistenceService.failIfPending(orderId, FailureReasons.STOCK_UNAVAILABLE);

        if (failed.isEmpty()) {
            log.info("Payment orderId={} bukan PENDING — lewati void VA (belum ada VA / sudah final)",
                    orderId);
            return;
        }

        try {
            midtransClient.cancel(orderId);
            log.info("VA order {} dibatalkan di Midtrans (stok tak tersedia)", orderId);
        } catch (Exception e) {
            log.error("Gagal void VA order {} di Midtrans (payment sudah ditandai FAILED)", orderId, e);
        }
    }
}
