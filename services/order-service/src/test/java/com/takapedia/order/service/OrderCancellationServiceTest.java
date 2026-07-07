package com.takapedia.order.service;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.entity.PaymentMethod;
import com.takapedia.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderCancellationService service;

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

    @Test
    void cancelDueToStockFailure_setsCancelled_whenWaitingPayment() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(waitingOrder(orderId)));

        service.cancelDueToStockFailure(orderId);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelDueToStockFailure_skips_whenOrderAlreadyPaid() {
        UUID orderId = UUID.randomUUID();
        Order order = waitingOrder(orderId);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        service.cancelDueToStockFailure(orderId);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelDueToStockFailure_doesNotThrow_whenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        service.cancelDueToStockFailure(orderId);

        verify(orderRepository, never()).save(any());
    }
}
