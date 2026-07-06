package com.takapedia.payment.webhook;

import java.util.UUID;

/**
 * Payload webhook Midtrans.
 *
 * transactionStatus disimpan sebagai String mentah (bukan enum) karena
 * signature di-hash dari string apa adanya yang dikirim Midtrans. Enum
 * dihitung on-demand via status() untuk keperluan logika.
 */
public record MidtransWebhookPayload(
        UUID orderId,
        String transactionStatus,
        String signatureKey
) {
    public MidtransTransactionStatus status() {
        return MidtransTransactionStatus.fromValue(transactionStatus);
    }
}