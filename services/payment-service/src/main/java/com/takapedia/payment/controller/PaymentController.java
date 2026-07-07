package com.takapedia.payment.controller;

import com.takapedia.payment.dto.PaymentResponse;
import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;

    /**
     * Opsi A (lihat TECH_DEBT "Authorization granular"): endpoint cukup authenticated.
     * Belum mengecek kepemilikan — user login manapun bisa melihat payment order siapa pun.
     * Otorisasi granular (userId JWT == userId payment) ditunda karena Payment belum simpan userId.
     */
    @GetMapping("/{orderId}")
    public PaymentResponse getByOrderId(@PathVariable UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment untuk order %s tidak ditemukan".formatted(orderId)));
        return PaymentResponse.from(payment);
    }
}
