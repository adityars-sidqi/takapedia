package com.takapedia.order.dto;

import com.takapedia.order.entity.Order;
import com.takapedia.order.entity.OrderStatus;

import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus());
    }
}