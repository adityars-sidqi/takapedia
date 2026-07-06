package com.takapedia.payment.webhook;

import java.util.UUID;

public record MidtransWebhookPayload(
        UUID orderId,
        MidtransTransactionStatus transactionStatus,
        String signatureKey
) {}