package com.takapedia.payment.integration.midtrans;

import com.takapedia.payment.exception.MidtransClient4xxException;
import com.takapedia.payment.exception.MidtransServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

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
        } catch (HttpClientErrorException e) {
            log.error("Midtrans charge rejected (4xx) for orderId={}, status={}",
                    request.orderId(), e.getStatusCode(), e);
            throw new MidtransClient4xxException(
                    "Midtrans rejected charge for orderId=" + request.orderId() +
                            " with status " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            log.error("Midtrans charge failed (5xx) for orderId={}, status={}",
                    request.orderId(), e.getStatusCode(), e);
            throw new MidtransServerException(
                    "Midtrans server error for orderId=" + request.orderId() +
                            " with status " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            log.error("Midtrans charge unreachable for orderId={}", request.orderId(), e);
            throw new MidtransServerException(
                    "Midtrans unreachable for orderId=" + request.orderId(), e);
        } catch (RestClientException e) {
            // Safety net: partial response, connection reset saat parsing body,
            // atau kegagalan HTTP-related lain yang belum ter-antisipasi.
            // Perlakukan sebagai server-side problem — trigger Circuit Breaker.
            log.error("Midtrans charge failed with unexpected error for orderId={}",
                    request.orderId(), e);
            throw new MidtransServerException(
                    "Midtrans call failed unexpectedly for orderId=" + request.orderId(), e);
        }
    }
}