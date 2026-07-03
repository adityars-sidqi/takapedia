package com.takapedia.payment.integration.midtrans;

import com.takapedia.payment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record MidtransChargeRequest(
        UUID orderId,
        BigDecimal amount,
        PaymentMethod paymentMethod
) {}