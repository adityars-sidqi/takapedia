package com.takapedia.order.service;

import com.takapedia.order.event.OrderCreatedEvent;

public interface OrderEventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
}