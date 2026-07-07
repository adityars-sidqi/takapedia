package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.event.OrderCreatedEvent;
import com.takapedia.payment.event.PaymentCompletedEvent;
import com.takapedia.payment.event.PaymentEventPublisher;
import com.takapedia.payment.event.PaymentOutcome;
import com.takapedia.payment.exception.MidtransClient4xxException;
import com.takapedia.payment.exception.MidtransServerException;
import com.takapedia.payment.config.PaymentProperties;
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
    private final PaymentProperties paymentProperties;

    public void initializePayment(OrderCreatedEvent event) {
        // Transaction 1: idempotency + Payment INITIATED
        Payment payment = paymentPersistenceService.initializePaymentRecord(event);

        // Call Midtrans DI LUAR transaction untuk bikin VA.
        // Kita dikte umur VA lewat custom_expiry; expiry_time di response yang jadi acuan.
        MidtransChargeRequest chargeRequest = new MidtransChargeRequest(
                event.orderId(), event.amount(), event.paymentMethod(),
                new MidtransChargeRequest.CustomExpiry(
                        paymentProperties.vaExpiry().toMinutes(), "minute"));

        try {
            MidtransChargeResponse response = midtransClient.charge(chargeRequest);

            // Charge sukses = VA dibuat. Payment → PENDING, nunggu user bayar.
            // TIDAK publish PaymentCompleted — order tetap WAITING_PAYMENT.
            // Konfirmasi pembayaran akan datang lewat webhook Midtrans.
            // expiresAt diambil dari response Midtrans (source of truth).
            paymentPersistenceService.markPending(
                    payment.getOrderId(), response.externalReference(), response.expiryTime());

        } catch (MidtransServerException e) {
            // Gagal bikin VA (gateway down / circuit open). VA tidak tersedia,
            // order tidak bisa lanjut → FAILED + kabari Order Service segera.
            log.warn("Midtrans server error for orderId={}, marking payment as FAILED",
                    payment.getOrderId());
            paymentPersistenceService.finalizeFailedPayment(
                    payment.getOrderId(), FailureReasons.GATEWAY_UNAVAILABLE);
            publishFailedEvent(payment.getOrderId(), FailureReasons.GATEWAY_UNAVAILABLE);

        } catch (MidtransClient4xxException e) {
            // Request kita ditolak gateway (4xx). Juga gagal bikin VA → FAILED.
            log.warn("Midtrans rejected charge for orderId={}, marking payment as FAILED",
                    payment.getOrderId());
            paymentPersistenceService.finalizeFailedPayment(
                    payment.getOrderId(), FailureReasons.REJECTED_BY_GATEWAY);
            publishFailedEvent(payment.getOrderId(), FailureReasons.REJECTED_BY_GATEWAY);
        }
    }

    private void publishFailedEvent(UUID orderId, String failureReason) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(),
                orderId,
                PaymentOutcome.FAILED,
                null,
                failureReason,
                Instant.now()
        );
        paymentEventPublisher.publish(event);
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