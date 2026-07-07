package com.takapedia.order.service;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.entity.PaymentMethod;
import com.takapedia.order.event.PaymentCompletedEvent;
import com.takapedia.order.event.PaymentOutcome;
import com.takapedia.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentUpdateServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderPaymentUpdateService service;

    private Order waitingOrder(UUID orderId) {
        Order order = new Order();
        order.setId(orderId);
        order.setProductId(UUID.randomUUID());
        order.setQuantity(3);
        order.setPaymentMethod(PaymentMethod.BCA_VA);
        order.setAmount(BigDecimal.valueOf(30_000));
        order.setStatus(OrderStatus.WAITING_PAYMENT);
        return order;
    }

    private PaymentCompletedEvent successEvent(UUID orderId) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(), orderId, PaymentOutcome.SUCCESS,
                "VA-BCA-12345678", null,
                Instant.parse("2026-07-04T10:00:00Z"));
    }

    private PaymentCompletedEvent failedEvent(UUID orderId) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(), orderId, PaymentOutcome.FAILED,
                null, "GATEWAY_UNAVAILABLE",
                Instant.parse("2026-07-04T10:00:00Z"));
    }

    private PaymentCompletedEvent expiredEvent(UUID orderId) {
        return new PaymentCompletedEvent(
                UUID.randomUUID(), orderId, PaymentOutcome.EXPIRED,
                null, "EXPIRE",
                Instant.parse("2026-07-04T10:00:00Z"));
    }

    @Test
    void applyPaymentOutcome_transitionsToPaid_whenSuccessAndOrderIsWaiting() {
        UUID orderId = UUID.randomUUID();
        Order order = waitingOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentOutcome(successEvent(orderId));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void applyPaymentOutcome_transitionsToPaymentFailed_whenFailedAndOrderIsWaiting() {
        UUID orderId = UUID.randomUUID();
        Order order = waitingOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentOutcome(failedEvent(orderId));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void applyPaymentOutcome_transitionsToExpired_whenExpiredAndOrderIsWaiting() {
        UUID orderId = UUID.randomUUID();
        Order order = waitingOrder(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentOutcome(expiredEvent(orderId));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    void applyPaymentOutcome_skipsUpdate_whenOrderAlreadyPaid() {
        UUID orderId = UUID.randomUUID();
        Order order = waitingOrder(orderId);
        order.setStatus(OrderStatus.PAID); // sudah PAID
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentOutcome(successEvent(orderId));

        // Nggak ada save, idempotent
        verify(orderRepository, never()).save(any());
    }

    @Test
    void applyPaymentOutcome_skipsUpdate_whenOrderAlreadyPaymentFailed() {
        UUID orderId = UUID.randomUUID();
        Order order = waitingOrder(orderId);
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.applyPaymentOutcome(failedEvent(orderId));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void applyPaymentOutcome_doesNotThrow_whenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Nggak throw
        service.applyPaymentOutcome(successEvent(orderId));

        verify(orderRepository, never()).save(any());
    }
}