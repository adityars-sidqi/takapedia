package com.takapedia.order.event;

import com.takapedia.order.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID productId,
        int quantity,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        Instant occurredAt
) {}