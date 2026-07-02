package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.entity.ProcessedEvent;
import com.takapedia.payment.event.OrderCreatedEvent;
import com.takapedia.payment.exception.AlreadyProcessedException;
import com.takapedia.payment.repository.PaymentRepository;
import com.takapedia.payment.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public void initializePayment(OrderCreatedEvent event) {
        // Langkah 1: catat event sebagai "sudah diproses" via insert.
        // Karena orderId adalah PRIMARY KEY di processed_events,
        // insert duplikat akan throw DataIntegrityViolationException secara atomik.
        // DB yang menegakkan keunikan — bukan cek-lalu-simpan manual.
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setOrderId(event.orderId());
        processedEvent.setProcessedAt(event.occurredAt());

        try {
            processedEventRepository.saveAndFlush(processedEvent);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyProcessedException(event.orderId());
        }

        // Langkah 2: insert berhasil = event baru. Bikin Payment record.
        Payment payment = new Payment();
        payment.setOrderId(event.orderId());
        payment.setAmount(event.amount());
        payment.setPaymentMethod(event.paymentMethod());
        payment.setStatus(PaymentStatus.INITIATED);

        paymentRepository.save(payment);
    }
}