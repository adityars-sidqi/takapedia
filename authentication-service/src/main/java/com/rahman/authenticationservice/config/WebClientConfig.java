package com.rahman.authenticationservice.config;

import com.rahman.authenticationservice.properties.KeyCloakProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class WebClientConfig {

    private final KeyCloakProperties keyCloakProperties;

    @Autowired
    public WebClientConfig(KeyCloakProperties keyCloakProperties) {
        this.keyCloakProperties = keyCloakProperties;
    }

    @Bean
    public WebClient keycloakWebClient() {
        return WebClient.builder()
                .baseUrl(keyCloakProperties.getServerUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }



    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response Status code: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
