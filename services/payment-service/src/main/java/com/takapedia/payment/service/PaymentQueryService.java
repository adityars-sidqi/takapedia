package com.takapedia.payment.service;

import com.takapedia.payment.dto.PaymentResponse;
import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Read path untuk endpoint polling GET /payments/{orderId}.
 *
 * Di-cache (Redis) karena client mem-poll orderId yang SAMA berulang-ulang selama
 * menunggu user membayar VA — read amplification tinggi pada key yang sama.
 * TTL pendek membatasi staleness; @CacheEvict di titik transisi status (webhook,
 * reconciliation, markPending, finalizeFailed) membuat perubahan langsung terlihat.
 * 404 tidak masuk cache karena exception tidak di-cache oleh @Cacheable.
 */
@Service
@RequiredArgsConstructor
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;

    @Cacheable(cacheNames = "payments", key = "#orderId")
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment untuk order %s tidak ditemukan".formatted(orderId)));
        return PaymentResponse.from(payment);
    }
}
