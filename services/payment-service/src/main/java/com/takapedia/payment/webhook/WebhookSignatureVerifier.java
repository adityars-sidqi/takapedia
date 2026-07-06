package com.takapedia.payment.webhook;

import com.takapedia.payment.integration.midtrans.MidtransProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Memverifikasi signature webhook Midtrans.
 *
 * Signature = SHA512(orderId + transactionStatus + serverKey).
 * Ini mencegah pihak yang tidak berwenang mengirim webhook palsu
 * ("order X sudah dibayar") tanpa mengetahui serverKey rahasia.
 *
 * Catatan: transactionStatus di-hash sebagai string lowercase apa adanya
 * yang dikirim Midtrans — verifier menerima raw string, bukan enum, agar
 * signature cocok byte-per-byte dengan yang dihitung pengirim.
 */
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifier {

    private final MidtransProperties midtransProperties;

    public boolean isValid(UUID orderId, String rawTransactionStatus, String providedSignature) {
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }
        String expected = computeSignature(orderId, rawTransactionStatus);
        // Bandingkan dengan constant-time comparison untuk menghindari timing attack
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8));
    }

    public String computeSignature(UUID orderId, String rawTransactionStatus) {
        String raw = orderId.toString() + rawTransactionStatus + midtransProperties.serverKey();
        return sha512Hex(raw);
    }

    private String sha512Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-512 selalu tersedia di JVM standar — ini seharusnya tidak pernah terjadi
            throw new IllegalStateException("SHA-512 algorithm not available", e);
        }
    }
}