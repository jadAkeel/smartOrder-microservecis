package com.jadakeel.apigateway.security;

import com.jadakeel.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final String secret;
    private final List<String> publicPaths = List.of("/api/auth/");

    public JwtAuthFilter(@Value("${auth.jwt.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        boolean isPublic = publicPaths.stream().anyMatch(path::startsWith);
        if (isPublic) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!JwtUtil.isValid(token, secret)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                .header("X-Authenticated-User-Id", JwtUtil.getSubject(token));
        String role = JwtUtil.getRole(token);
        if (role != null) {
            requestBuilder.header("X-Authenticated-Role", role);
        }

        ServerWebExchange authenticatedExchange = exchange.mutate()
                .request(requestBuilder.build())
                .build();

        return chain.filter(authenticatedExchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
