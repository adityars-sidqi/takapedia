package com.takapedia.apigateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class UserKeyResolverTest {

    private final KeyResolver resolver = new UserKeyResolver();

    @Test
    void withUserIdHeader_usesUserId() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products/123")
                .header("X-User-Id", "user-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("user-123")
                .verifyComplete();
    }

    @Test
    void withoutUserIdHeader_usesIpAddress() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products/123")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.50", 12345))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("192.168.1.50")
                .verifyComplete();
    }
}