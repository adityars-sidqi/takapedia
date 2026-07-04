package com.takapedia.payment.event;

public interface PaymentEventPublisher {

    void publish(PaymentCompletedEvent event);
}