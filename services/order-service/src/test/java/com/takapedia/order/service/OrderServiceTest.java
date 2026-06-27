package com.takapedia.order.service;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.event.OrderCreatedEvent;
import com.takapedia.order.event.OrderEventPublisher;
import com.takapedia.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_savesPendingOrderAndPublishesEvent() {
        UUID productId = UUID.randomUUID();
        int quantity = 3;

        // repository.save mengembalikan order dengan id ter-generate
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        Order result = orderService.createOrder(productId, quantity);

        // 1. Order tersimpan dengan status PENDING
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getQuantity()).isEqualTo(quantity);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);

        // 2. Event diterbitkan dengan data yang benar
        ArgumentCaptor<OrderCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventPublisher).publish(eventCaptor.capture());
        OrderCreatedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(result.getId());
        assertThat(event.productId()).isEqualTo(productId);
        assertThat(event.quantity()).isEqualTo(quantity);
    }
}