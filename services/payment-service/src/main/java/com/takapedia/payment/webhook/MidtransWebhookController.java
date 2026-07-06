package com.takapedia.payment.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/midtrans")
@RequiredArgsConstructor
@Slf4j
public class MidtransWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookProcessingService webhookProcessingService;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody MidtransWebhookPayload payload) {
        // 1. Verifikasi signature — pakai raw transactionStatus string
        boolean valid = signatureVerifier.isValid(
                payload.orderId(),
                payload.transactionStatus(),
                payload.signatureKey());

        if (!valid) {
            log.warn("Invalid webhook signature for orderId={}", payload.orderId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Proses transisi status (idempotent)
        WebhookProcessingService.ProcessResult result =
                webhookProcessingService.process(payload.orderId(), payload.status());

        // 3. Selalu return 200 untuk signature valid (termonasuk not-found & skip),
        //    supaya Midtrans tidak retry storm. Log sudah mencatat detail.
        return ResponseEntity.ok().build();
    }
}