package com.takapedia.delivery.service;

import com.takapedia.delivery.entity.Shipment;
import com.takapedia.delivery.entity.ShipmentStatus;
import com.takapedia.delivery.event.OrderPaidEvent;
import com.takapedia.delivery.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @InjectMocks
    private ShipmentService service;

    private OrderPaidEvent event(UUID orderId) {
        return new OrderPaidEvent(
                orderId,
                UUID.randomUUID(),
                2,
                "Jl. Mawar No. 1",
                BigDecimal.valueOf(20_000),
                Instant.parse("2026-07-04T10:00:00Z"));
    }

    @Test
    void createFromOrderPaid_createsPendingShipment_whenNew() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.existsById(orderId)).thenReturn(false);

        service.createFromOrderPaid(event(orderId));

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        Shipment saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        assertThat(saved.getShippingAddress()).isEqualTo("Jl. Mawar No. 1");
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void createFromOrderPaid_skips_whenShipmentAlreadyExists() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.existsById(orderId)).thenReturn(true);

        service.createFromOrderPaid(event(orderId));

        verify(shipmentRepository, never()).save(any());
    }

    private Shipment shipment(UUID orderId, ShipmentStatus status) {
        Shipment s = new Shipment();
        s.setOrderId(orderId);
        s.setProductId(UUID.randomUUID());
        s.setQuantity(2);
        s.setShippingAddress("Jl. Mawar No. 1");
        s.setStatus(status);
        return s;
    }

    @Test
    void markShipped_transitionsPendingToShipped() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findById(orderId))
                .thenReturn(Optional.of(shipment(orderId, ShipmentStatus.PENDING)));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = service.markShipped(orderId);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
    }

    @Test
    void markShipped_conflict_whenNotPending() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findById(orderId))
                .thenReturn(Optional.of(shipment(orderId, ShipmentStatus.SHIPPED)));

        assertThatThrownBy(() -> service.markShipped(orderId))
                .isInstanceOf(ResponseStatusException.class);
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void markShipped_notFound_whenMissing() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markShipped(orderId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void markDelivered_transitionsShippedToDelivered() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findById(orderId))
                .thenReturn(Optional.of(shipment(orderId, ShipmentStatus.SHIPPED)));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Shipment result = service.markDelivered(orderId);

        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }
}
