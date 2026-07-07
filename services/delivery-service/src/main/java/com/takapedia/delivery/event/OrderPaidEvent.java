package com.takapedia.delivery.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mirror kontrak dari Order Service (Java). Diterbitkan saat order PAID.
 */
public record OrderPaidEvent(
        UUID orderId,
        UUID productId,
        int quantity,
        String shippingAddress,
        BigDecimal amount,
        Instant occurredAt
) {}
