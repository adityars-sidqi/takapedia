package com.takapedia.payment.event;

import java.time.Instant;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        UUID orderId,
        PaymentOutcome outcome,
        String externalReference,
        String failureReason,
        Instant occurredAt
) {}