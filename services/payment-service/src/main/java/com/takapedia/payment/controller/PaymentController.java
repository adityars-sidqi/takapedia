package com.takapedia.payment.controller;

import com.takapedia.payment.dto.PaymentResponse;
import com.takapedia.payment.service.PaymentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentQueryService paymentQueryService;

    /**
     * Opsi A (lihat TECH_DEBT "Authorization granular"): endpoint cukup authenticated.
     * Belum mengecek kepemilikan — user login manapun bisa melihat payment order siapa pun.
     * Read di-cache di PaymentQueryService (endpoint ini sering di-poll saat menunggu bayar).
     */
    @GetMapping("/{orderId}")
    public PaymentResponse getByOrderId(@PathVariable UUID orderId) {
        return paymentQueryService.getByOrderId(orderId);
    }
}
