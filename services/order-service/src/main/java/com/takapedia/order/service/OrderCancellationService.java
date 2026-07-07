package com.takapedia.order.service;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Membatalkan order sebagai kompensasi saat Inventory gagal reserve (stok kurang).
 * Idempotent: hanya membatalkan jika order masih WAITING_PAYMENT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancellationService {

    private final OrderRepository orderRepository;

    @Transactional
    public void cancelDueToStockFailure(UUID orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.warn("StockReservationFailed untuk orderId={} yang tidak dikenal", orderId);
            return;
        }

        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            log.info("Order {} sudah berstatus {}, lewati pembatalan stock-failure",
                    orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}
