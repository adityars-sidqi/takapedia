package com.takapedia.order.service;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;
import com.takapedia.order.entity.PaymentMethod;
import com.takapedia.order.event.OrderCreatedEvent;
import com.takapedia.order.event.OrderEventPublisher;
import com.takapedia.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public Order createOrder(UUID productId, int quantity, PaymentMethod paymentMethod) {
        Order order = new Order();
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.WAITING_PAYMENT);

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getPaymentMethod(),
                savedOrder.getCreatedAt()
        );
        orderEventPublisher.publish(event);

        return savedOrder;
    }
}