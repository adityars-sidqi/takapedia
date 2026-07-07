package com.takapedia.payment.webhook;

import com.takapedia.payment.event.PaymentCompletedEvent;
import com.takapedia.payment.event.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks/midtrans")
@RequiredArgsConstructor
@Slf4j
public class MidtransWebhookController {

    private final WebhookSignatureVerifier signatureVerifier;
    private final WebhookProcessingService webhookProcessingService;
    private final PaymentEventPublisher paymentEventPublisher;

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

        // 2. Proses transisi status (idempotent, dalam transaction)
        WebhookProcessingResult result =
                webhookProcessingService.process(payload.orderId(), payload.status());

        // 3. Publish PaymentCompleted DI LUAR transaction, hanya jika transisi terjadi.
        //    Konsisten dengan Fase 5a: publish setelah DB commit, bukan di dalamnya.
        if (result.outcome() == WebhookProcessingResult.Outcome.TRANSITIONED) {
            publishCompletedEvent(payload.orderId(), result);
        }

        // 4. Selalu 200 untuk signature valid (termasuk skip & not-found).
        return ResponseEntity.ok().build();
    }

    private void publishCompletedEvent(UUID orderId, WebhookProcessingResult result) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                result.paymentOutcome(),
                result.externalReference(),
                result.failureReason(),
                Instant.now()
        );
        paymentEventPublisher.publish(event);
        log.info("Published PaymentCompleted for orderId={}, outcome={}",
                orderId, result.paymentOutcome());
    }
}