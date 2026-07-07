package com.takapedia.order.entity;

public enum OrderStatus {
    WAITING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    EXPIRED,
    CANCELLED
}