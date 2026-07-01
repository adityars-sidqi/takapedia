package com.takapedia.apigateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final ReactiveJwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst("Authorization");

        // Selalu strip header identitas dari client (zero-trust)
        ServerHttpRequest stripped = request.mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                })
                .build();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Tak ada token → teruskan tanpa identitas (downstream yang menolak kalau perlu)
            return chain.filter(exchange.mutate().request(stripped).build());
        }

        String token = authHeader.substring(7);

        // Decode via JWKS (reactive). Kalau invalid, teruskan tanpa identitas (Mazhab 2).
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    ServerHttpRequest mutated = stripped.mutate()
                            .headers(h -> {
                                h.set("X-User-Id", jwt.getSubject());
                                h.set("X-User-Role", jwt.getClaimAsString("role"));
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .onErrorResume(e ->
                        // Token invalid → teruskan tanpa identitas, tidak menolak
                        chain.filter(exchange.mutate().request(stripped).build()));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}