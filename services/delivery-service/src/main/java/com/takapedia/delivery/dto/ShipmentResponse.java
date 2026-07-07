package com.takapedia.delivery.dto;

import com.takapedia.delivery.entity.Shipment;
import com.takapedia.delivery.entity.ShipmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ShipmentResponse(
        UUID orderId,
        UUID productId,
        int quantity,
        String shippingAddress,
        ShipmentStatus status,
        Instant createdAt
) {
    public static ShipmentResponse from(Shipment s) {
        return new ShipmentResponse(
                s.getOrderId(),
                s.getProductId(),
                s.getQuantity(),
                s.getShippingAddress(),
                s.getStatus(),
                s.getCreatedAt()
        );
    }
}
