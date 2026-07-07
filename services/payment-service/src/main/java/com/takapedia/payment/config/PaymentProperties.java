package com.takapedia.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Konfigurasi domain Payment.
 * vaExpiry: umur VA yang kita dikte ke Midtrans (custom_expiry) saat charge.
 * reconciliation.grace: buffer sebelum job berani men-EXPIRED — job hanya scan
 *   Payment yang expiresAt-nya sudah lewat lebih dari grace, agar tak mendahului webhook Midtrans.
 */
@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(Duration vaExpiry, Reconciliation reconciliation) {

    public record Reconciliation(Duration grace) {}
}
