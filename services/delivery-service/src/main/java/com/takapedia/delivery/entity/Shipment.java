package com.takapedia.delivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Shipment per order. orderId sebagai primary key = kunci idempotency:
 * OrderPaid yang sama (redelivery) tidak membuat shipment ganda.
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
public class Shipment {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
