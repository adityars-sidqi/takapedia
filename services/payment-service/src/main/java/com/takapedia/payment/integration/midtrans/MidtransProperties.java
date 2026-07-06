package com.takapedia.payment.integration.midtrans;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "midtrans")
public record MidtransProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String serverKey
) {}