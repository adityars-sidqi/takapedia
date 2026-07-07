package com.takapedia.payment.event;

import java.util.UUID;

/**
 * Mirror kontrak dari Inventory Service (Go). Payment hanya butuh orderId untuk void VA;
 * field productId/quantity/reason lain diabaikan (Spring Boot default: ignore unknown).
 */
public record StockReservationFailedEvent(
        UUID orderId
) {}
