package com.takapedia.order.event;

public interface OrderEventPublisher {
    void publish(OrderCreatedEvent event);
}