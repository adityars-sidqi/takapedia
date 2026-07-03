package com.takapedia.payment.integration.midtrans;

public interface MidtransClient {

    MidtransChargeResponse charge(MidtransChargeRequest request);
}