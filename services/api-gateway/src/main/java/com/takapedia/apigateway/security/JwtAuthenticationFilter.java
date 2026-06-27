package com.takapedia.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst("Authorization");

        ServerHttpRequest.Builder mutated = request.mutate().headers(headers -> {
            // strip dulu: header identitas dari client tidak boleh dipercaya
            headers.remove("X-User-Id");
            headers.remove("X-User-Role");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtService.isTokenValid(token)) {
                    headers.set("X-User-Id", jwtService.extractUserId(token));
                    headers.set("X-User-Role", jwtService.extractRole(token));
                }
            }
        });

        return chain.filter(exchange.mutate().request(mutated.build()).build());
    }

    @Override
    public int getOrder() {
        return -1;  // jalan lebih awal, sebelum filter routing
    }
}