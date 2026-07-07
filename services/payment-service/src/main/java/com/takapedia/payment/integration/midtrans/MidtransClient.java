package com.takapedia.payment.integration.midtrans;

import java.util.UUID;

public interface MidtransClient {

    MidtransChargeResponse charge(MidtransChargeRequest request);

    /**
     * Membatalkan (void) transaksi/VA di Midtrans. Dipakai saat kompensasi
     * (mis. stok tak tersedia) untuk menutup VA yang terlanjur dibuat.
     */
    void cancel(UUID orderId);
}
