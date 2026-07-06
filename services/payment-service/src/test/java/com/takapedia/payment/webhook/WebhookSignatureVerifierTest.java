package com.takapedia.payment.webhook;

import com.takapedia.payment.integration.midtrans.MidtransProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {

    private static final String SERVER_KEY = "SB-Mid-server-DUMMYKEYFORLOCAL0000";

    private WebhookSignatureVerifier verifier;

    @BeforeEach
    void setUp() {
        MidtransProperties props = new MidtransProperties(
                "http://localhost:9999",
                Duration.ofSeconds(3),
                Duration.ofSeconds(5),
                SERVER_KEY
        );
        verifier = new WebhookSignatureVerifier(props);
    }

    @Test
    void isValid_returnsTrue_forCorrectlyComputedSignature() {
        UUID orderId = UUID.randomUUID();
        String status = "settlement";

        // Signature yang dihitung dengan serverKey yang benar
        String validSignature = verifier.computeSignature(orderId, status);

        assertThat(verifier.isValid(orderId, status, validSignature)).isTrue();
    }

    @Test
    void isValid_returnsFalse_forTamperedSignature() {
        UUID orderId = UUID.randomUUID();
        String status = "settlement";

        assertThat(verifier.isValid(orderId, status, "tampered-signature-value")).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenStatusDiffersFromSignedStatus() {
        UUID orderId = UUID.randomUUID();

        // Signature dihitung untuk "settlement"
        String signatureForSettlement = verifier.computeSignature(orderId, "settlement");

        // Tapi diverifikasi terhadap status "expire" → harus gagal
        assertThat(verifier.isValid(orderId, "expire", signatureForSettlement)).isFalse();
    }

    @Test
    void isValid_returnsFalse_whenOrderIdDiffersFromSignedOrderId() {
        UUID signedOrderId = UUID.randomUUID();
        UUID differentOrderId = UUID.randomUUID();

        String signature = verifier.computeSignature(signedOrderId, "settlement");

        assertThat(verifier.isValid(differentOrderId, "settlement", signature)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forNullOrBlankSignature() {
        UUID orderId = UUID.randomUUID();

        assertThat(verifier.isValid(orderId, "settlement", null)).isFalse();
        assertThat(verifier.isValid(orderId, "settlement", "")).isFalse();
        assertThat(verifier.isValid(orderId, "settlement", "   ")).isFalse();
    }

    @Test
    void computeSignature_isDeterministic_forSameInput() {
        UUID orderId = UUID.randomUUID();

        String sig1 = verifier.computeSignature(orderId, "settlement");
        String sig2 = verifier.computeSignature(orderId, "settlement");

        assertThat(sig1).isEqualTo(sig2);
        // SHA-512 hex = 128 karakter
        assertThat(sig1).hasSize(128);
    }
}