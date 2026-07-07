package com.takapedia.payment.dto;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID orderId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String externalReference,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getOrderId(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getStatus(),
                p.getExternalReference(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
