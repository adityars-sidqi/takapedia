package com.takapedia.order.service;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.event.PaymentCompletedEvent;
import com.takapedia.order.event.PaymentOutcome;
import com.takapedia.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentUpdateService {

    private final OrderRepository orderRepository;

    /**
     * Update Order status berdasarkan PaymentCompletedEvent.
     * Idempotent: hanya update jika status masih WAITING_PAYMENT.
     * Kalau order sudah PAID atau PAYMENT_FAILED, event diabaikan (log & skip).
     * Kalau order tidak ditemukan, log warning (kemungkinan event untuk order yang
     * belum di-commit, atau order-nya beneran hilang — worth investigate).
     */
    @Transactional
    public void applyPaymentOutcome(PaymentCompletedEvent event) {
        Optional<Order> orderOpt = orderRepository.findById(event.orderId());

        if (orderOpt.isEmpty()) {
            log.warn("Received PaymentCompletedEvent for unknown orderId={}", event.orderId());
            return;
        }

        Order order = orderOpt.get();

        if (order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            log.info("Order {} already in status {}, skipping payment outcome update",
                    event.orderId(), order.getStatus());
            return;
        }

        OrderStatus newStatus = event.outcome() == PaymentOutcome.SUCCESS
                ? OrderStatus.PAID
                : OrderStatus.PAYMENT_FAILED;

        order.setStatus(newStatus);
        orderRepository.save(order);
    }
}