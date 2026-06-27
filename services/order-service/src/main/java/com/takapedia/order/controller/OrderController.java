package com.takapedia.order.controller;

import com.takapedia.order.dto.CreateOrderRequest;
import com.takapedia.order.dto.OrderResponse;
import com.takapedia.order.entity.Order;
import com.takapedia.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.createOrder(request.productId(), request.quantity());
        return OrderResponse.from(order);
    }
}