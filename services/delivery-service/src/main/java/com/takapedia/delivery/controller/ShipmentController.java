package com.takapedia.delivery.controller;

import com.takapedia.delivery.dto.ShipmentResponse;
import com.takapedia.delivery.entity.Shipment;
import com.takapedia.delivery.repository.ShipmentRepository;
import com.takapedia.delivery.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentRepository shipmentRepository;

    @GetMapping("/{orderId}")
    public ShipmentResponse get(@PathVariable UUID orderId) {
        Shipment shipment = shipmentRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Shipment untuk order %s tidak ditemukan".formatted(orderId)));
        return ShipmentResponse.from(shipment);
    }

    @PostMapping("/{orderId}/ship")
    public ShipmentResponse ship(@PathVariable UUID orderId) {
        return ShipmentResponse.from(shipmentService.markShipped(orderId));
    }

    @PostMapping("/{orderId}/deliver")
    public ShipmentResponse deliver(@PathVariable UUID orderId) {
        return ShipmentResponse.from(shipmentService.markDelivered(orderId));
    }
}
