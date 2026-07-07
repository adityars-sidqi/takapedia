package com.takapedia.delivery.service;

import com.takapedia.delivery.entity.Shipment;
import com.takapedia.delivery.entity.ShipmentStatus;
import com.takapedia.delivery.event.OrderPaidEvent;
import com.takapedia.delivery.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
