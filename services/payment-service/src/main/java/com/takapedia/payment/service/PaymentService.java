package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.event.OrderCreatedEvent;
import com.takapedia.payment.event.PaymentCompletedEvent;
import com.takapedia.payment.event.PaymentEventPublisher;
import com.takapedia.payment.event.PaymentOutcome;
import com.takapedia.payment.exception.MidtransClient4xxException;
import com.takapedia.payment.exception.MidtransServerException;
import com.takapedia.payment.integration.midtrans.MidtransChargeRequest;
import com.takapedia.payment.integration.midtrans.MidtransChargeResponse;
import com.takapedia.payment.integration.midtrans.MidtransClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentPersistenceService paymentPersistenceService;
    private final MidtransClient midtransClient;
    private final PaymentEventPublisher paymentEventPublisher;

    /**
     * Orchestrator method — TIDAK @Transactional.
     * Split ke 2 transaction (via PaymentPersistenceService) supaya
     * Midtrans HTTP call tidak menahan koneksi DB.
     */
    public void initializePayment(OrderCreatedEvent event) {
        // Transaction 1: idempotency + Payment INITIATED
        Payment payment = paymentPersistenceService.initializePaymentRecord(event);

        // Call Midtrans DI LUAR transaction
        MidtransChargeRequest chargeRequest = new MidtransChargeRequest(
                event.orderId(), event.amount(), event.paymentMethod());

        try {
            MidtransChargeResponse response = midtransClient.charge(chargeRequest);

            // Transaction 2 (sukses)
            paymentPersistenceService.finalizeSuccessfulPayment(
                    payment.getOrderId(), response.externalReference());

            publishCompletedEvent(payment.getOrderId(),
                    PaymentOutcome.SUCCESS, response.externalReference(), null);

        } catch (MidtransServerException e) {
            log.warn("Midtrans server error for orderId={}, marking payment as FAILED",
                    payment.getOrderId());
            // Transaction 2 (gagal)
            paymentPersistenceService.finalizeFailedPayment(
                    payment.getOrderId(), FailureReasons.GATEWAY_UNAVAILABLE);

            publishCompletedEvent(payment.getOrderId(),
                    PaymentOutcome.FAILED, null, FailureReasons.GATEWAY_UNAVAILABLE);

        } catch (MidtransClient4xxException e) {
            log.warn("Midtrans rejected charge for orderId={}, marking payment as FAILED",
                    payment.getOrderId());
            // Transaction 2 (gagal)
            paymentPersistenceService.finalizeFailedPayment(
                    payment.getOrderId(), FailureReasons.REJECTED_BY_GATEWAY);

            publishCompletedEvent(payment.getOrderId(),
                    PaymentOutcome.FAILED, null, FailureReasons.REJECTED_BY_GATEWAY);
        }
    }

    private void publishCompletedEvent(UUID orderId,
                                       PaymentOutcome outcome,
                                       String externalReference,
                                       String failureReason) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                outcome,
                externalReference,
                failureReason,
                Instant.now()
        );
        paymentEventPublisher.publish(event);
    }
}