package com.takapedia.payment.repository;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    /**
     * Kandidat reconciliation: Payment yang masih PENDING dan sudah melewati expiresAt.
     * Dipakai job untuk mendeteksi VA kedaluwarsa yang tidak dikonfirmasi via webhook.
     */
    List<Payment> findByStatusAndExpiresAtBefore(PaymentStatus status, Instant cutoff);

    /**
     * Conditional update atomik: ubah status HANYA jika status saat ini PENDING.
     * Mengembalikan jumlah row yang ter-update (1 = berhasil transisi, 0 = bukan PENDING).
     *
     * Ini menutup race window: alih-alih SELECT-lalu-UPDATE (yang punya celah antara
     * baca dan tulis), DB yang menegakkan kondisi "hanya jika PENDING" secara atomik.
     */
    @Modifying
    @Query("""
            UPDATE Payment p
            SET p.status = :newStatus,
                p.failureReason = :failureReason
            WHERE p.orderId = :orderId AND p.status = :expectedStatus
            """)
    int updateStatusIfCurrentMatches(
            @Param("orderId") UUID orderId,
            @Param("expectedStatus") PaymentStatus expectedStatus,
            @Param("newStatus") PaymentStatus newStatus,
            @Param("failureReason") String failureReason);
}