package com.takapedia.order.event;

public interface OrderPaidPublisher {

    void publish(OrderPaidEvent event);
}
