package com.takapedia.order.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID productId,
        int quantity,
        Instant occurredAt
) {}