package com.takapedia.payment.integration.midtrans;

import com.takapedia.payment.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

public record MidtransChargeRequest(
        UUID orderId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        CustomExpiry customExpiry
) {

    /**
     * Convenience constructor tanpa custom_expiry (Midtrans pakai default-nya).
     * Menjaga call-site lama tetap kompatibel.
     */
    public MidtransChargeRequest(UUID orderId, BigDecimal amount, PaymentMethod paymentMethod) {
        this(orderId, amount, paymentMethod, null);
    }

    /**
     * Mendikte umur VA ke Midtrans (mirror custom_expiry di Core API).
     * unit contoh: "minute". Kita kirim ini; expiry_time di response yang jadi acuan.
     */
    public record CustomExpiry(long duration, String unit) {}
}