package com.takapedia.payment.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Status transaksi dari webhook Midtrans. Midtrans mengirim nilai lowercase
 * (mis. "settlement"), sehingga deserialisasi dibuat case-insensitive via @JsonCreator.
 * Nilai tak dikenal dipetakan ke UNKNOWN agar handler bisa memutuskan (log & skip),
 * bukan gagal deserialisasi total.
 */
public enum MidtransTransactionStatus {
    SETTLEMENT,
    CAPTURE,
    EXPIRE,
    DENY,
    CANCEL,
    FAILURE,
    PENDING,
    UNKNOWN;

    @JsonCreator
    public static MidtransTransactionStatus fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return MidtransTransactionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}