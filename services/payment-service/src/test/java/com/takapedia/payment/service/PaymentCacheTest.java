package com.takapedia.payment.service;

import com.takapedia.payment.entity.Payment;
import com.takapedia.payment.entity.PaymentMethod;
import com.takapedia.payment.entity.PaymentStatus;
import com.takapedia.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Membuktikan cache-aside pada read path payment tanpa perlu Redis: cache.type=simple
 * memakai cache in-memory; PaymentRepository di-mock supaya hit-nya bisa dihitung.
 */
@SpringBootTest(properties = "spring.cache.type=simple")
@ActiveProfiles("test")
class PaymentCacheTest {

    @MockitoBean
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentQueryService paymentQueryService;

    @Autowired
    private PaymentPersistenceService paymentPersistenceService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("payments").clear();
    }

    private Payment sample(UUID orderId) {
        Payment p = new Payment();
        p.setOrderId(orderId);
        p.setAmount(BigDecimal.valueOf(30_000));
        p.setPaymentMethod(PaymentMethod.BCA_VA);
        p.setStatus(PaymentStatus.PENDING);
        p.setExternalReference("VA-BCA-12345678");
        return p;
    }

    @Test
    void getByOrderId_secondCallServedFromCache_repositoryHitOnce() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(sample(orderId)));

        paymentQueryService.getByOrderId(orderId);
        paymentQueryService.getByOrderId(orderId); // dari cache

        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void statusTransition_evictsCache_nextGetHitsRepositoryAgain() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(sample(orderId)));

        paymentQueryService.getByOrderId(orderId);              // cache (findByOrderId #1)
        paymentPersistenceService.expireIfStillPending(orderId); // @CacheEvict membuang entri
        paymentQueryService.getByOrderId(orderId);              // cache kosong → findByOrderId #2

        verify(paymentRepository, times(2)).findByOrderId(orderId);
    }
}
