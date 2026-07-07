package com.takapedia.order.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Diterbitkan Order Service saat order transisi ke PAID.
 * Membawa detail pengiriman supaya Delivery Service bisa membuat shipment.
 */
public record OrderPaidEvent(
        UUID orderId,
        UUID productId,
        int quantity,
        String shippingAddress,
        BigDecimal amount,
        Instant occurredAt
) {}
