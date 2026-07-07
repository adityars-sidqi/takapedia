package com.takapedia.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Mengaktifkan penjadwalan (@Scheduled) untuk reconciliation job dan mendaftarkan
 * PaymentProperties. Dipisah dari SecurityConfig agar tanggung jawab konfigurasi jelas.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PaymentProperties.class)
public class SchedulingConfig {
}
