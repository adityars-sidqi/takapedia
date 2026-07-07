package com.takapedia.payment.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Mengaktifkan Spring Cache abstraction. Backing store lewat properti spring.cache.type —
 * "redis" saat runtime, "none" saat test. Cache-aside diterapkan di PaymentQueryService,
 * invalidasi via @CacheEvict di titik transisi status Payment.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
