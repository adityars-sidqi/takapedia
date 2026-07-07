package com.takapedia.payment.webhook;

import com.takapedia.payment.event.PaymentOutcome;

public record WebhookProcessingResult(
        Outcome outcome,
        PaymentOutcome paymentOutcome,
        String externalReference,
        String failureReason
) {
    public enum Outcome {
        TRANSITIONED,
        SKIPPED,
        ORDER_NOT_FOUND
    }

    public static WebhookProcessingResult transitioned(
            PaymentOutcome paymentOutcome, String externalReference, String failureReason) {
        return new WebhookProcessingResult(
                Outcome.TRANSITIONED, paymentOutcome, externalReference, failureReason);
    }

    public static WebhookProcessingResult skipped() {
        return new WebhookProcessingResult(Outcome.SKIPPED, null, null, null);
    }

    public static WebhookProcessingResult orderNotFound() {
        return new WebhookProcessingResult(Outcome.ORDER_NOT_FOUND, null, null, null);
    }
}