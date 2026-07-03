package com.takapedia.payment.integration.midtrans;

public record MidtransChargeResponse(
        String externalReference,
        MidtransPaymentStatus status
) {}