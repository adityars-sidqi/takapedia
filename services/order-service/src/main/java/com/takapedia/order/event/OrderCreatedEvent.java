package com.takapedia.order.event;

import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID productId,
        int quantity
) {}