package com.takapedia.payment.integration.midtrans;

import java.time.Instant;

/**
 * Response charge dari Midtrans.
 * expiryTime: timestamp kedaluwarsa VA menurut Midtrans — SOURCE OF TRUTH.
 * Disimpan apa adanya ke Payment.expiresAt (bukan dihitung ulang di sisi kita).
 */
public record MidtransChargeResponse(
        String externalReference,
        MidtransPaymentStatus status,
        Instant expiryTime
) {}