package com.takapedia.delivery.service;

import com.takapedia.delivery.entity.Shipment;
import com.takapedia.delivery.entity.ShipmentStatus;
import com.takapedia.delivery.event.OrderPaidEvent;
import com.takapedia.delivery.repository.ShipmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
}
