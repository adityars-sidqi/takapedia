package com.takapedia.order.dto;

import com.takapedia.order.entity.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID productId,
        @Min(1) int quantity,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank String shippingAddress
) {}
