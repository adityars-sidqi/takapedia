package com.takapedia.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class RsaKeyConfig {

    @Bean
    public RSAPrivateKey rsaPrivateKey(@Value("${jwt.private-key}") Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return (RSAPrivateKey) RsaKeyConverters.pkcs8().convert(is);
        }
    }

    @Bean
    public RSAPublicKey rsaPublicKey(@Value("${jwt.public-key}") Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            return (RSAPublicKey) RsaKeyConverters.x509().convert(is);
        }
    }
}