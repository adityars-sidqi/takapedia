package com.takapedia.order.event;

import java.util.UUID;

/**
 * Mirror kontrak dari Inventory Service (Go): diterbitkan saat reserve gagal (stok kurang).
 * Field productId/quantity dari Inventory diabaikan (Spring Boot default: ignore unknown).
 */
public record StockReservationFailedEvent(
        UUID orderId,
        String reason
) {}
