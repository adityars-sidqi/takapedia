package com.takapedia.delivery.service;

import com.takapedia.delivery.entity.Shipment;
import com.takapedia.delivery.entity.ShipmentStatus;
import com.takapedia.delivery.event.OrderPaidEvent;
import com.takapedia.delivery.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    /**
     * Buat shipment PENDING dari OrderPaid. Idempotent lewat orderId (PK):
     * OrderPaid yang sama (redelivery) tidak membuat shipment ganda.
     * Kafka menjamin pesan ber-key sama (orderId) diproses berurutan oleh satu consumer,
     * jadi cek existsById cukup tanpa race untuk order yang sama.
     */
    @Transactional
    public void createFromOrderPaid(OrderPaidEvent event) {
        if (shipmentRepository.existsById(event.orderId())) {
            log.info("Shipment untuk order {} sudah ada, skip (idempotent)", event.orderId());
            return;
        }

        Shipment shipment = new Shipment();
        shipment.setOrderId(event.orderId());
        shipment.setProductId(event.productId());
        shipment.setQuantity(event.quantity());
        shipment.setShippingAddress(event.shippingAddress());
        shipment.setStatus(ShipmentStatus.PENDING);
        shipmentRepository.save(shipment);

        log.info("Shipment dibuat untuk order {} → {}", event.orderId(), event.shippingAddress());
    }

    /** Transisi PENDING → SHIPPED. 404 kalau tak ada, 409 kalau bukan PENDING. */
    @Transactional
    public Shipment markShipped(UUID orderId) {
        return transition(orderId, ShipmentStatus.PENDING, ShipmentStatus.SHIPPED);
    }

    /** Transisi SHIPPED → DELIVERED. 404 kalau tak ada, 409 kalau bukan SHIPPED. */
    @Transactional
    public Shipment markDelivered(UUID orderId) {
        return transition(orderId, ShipmentStatus.SHIPPED, ShipmentStatus.DELIVERED);
    }

    private Shipment transition(UUID orderId, ShipmentStatus expected, ShipmentStatus next) {
        Shipment shipment = shipmentRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment untuk order %s tidak ditemukan".formatted(orderId)));
        if (shipment.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Shipment %s berstatus %s, tidak bisa ke %s".formatted(orderId, shipment.getStatus(), next));
        }
        shipment.setStatus(next);
        Shipment saved = shipmentRepository.save(shipment);
        log.info("Shipment order {} → {}", orderId, next);
        return saved;
    }
}
