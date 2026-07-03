package com.takapedia.payment.integration.midtrans;

import com.takapedia.payment.exception.MidtransClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class MidtransRestClient implements MidtransClient {

    static final String CHARGE_ENDPOINT = "/v2/charge";

    private final RestClient restClient;

    public MidtransRestClient(RestClient midtransHttpClient) {
        this.restClient = midtransHttpClient;
    }

    @Override
    public MidtransChargeResponse charge(MidtransChargeRequest request) {
        try {
            return restClient.post()
                    .uri(CHARGE_ENDPOINT)
                    .body(request)
                    .retrieve()
                    .body(MidtransChargeResponse.class);
        } catch (RestClientException e) {
            log.error("Midtrans charge call failed for orderId={}", request.orderId(), e);
            throw new MidtransClientException(
                    "Failed to call Midtrans charge for orderId=" + request.orderId(), e);
        }
    }
}