package com.jadakeel.apigateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class GatewayIndexController {

    private static final List<String> API_ROUTES = List.of(
            "/api/auth",
            "/api/products",
            "/api/inventory",
            "/api/orders",
            "/api/payments",
            "/api/shipping",
            "/api/delivery",
            "/api/notifications"
    );

    @GetMapping({"/", "/api"})
    public Map<String, Object> index() {
        return Map.of(
                "service", "api-gateway",
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "routes", API_ROUTES,
                "health", "/actuator/health"
        );
    }
}
