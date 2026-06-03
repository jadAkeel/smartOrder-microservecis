package com.jadakeel.order.security;

import com.jadakeel.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OrderAuthenticationFilter extends OncePerRequestFilter {

    private final String secret;

    public OrderAuthenticationFilter(@Value("${auth.jwt.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean requiresAuth = "POST".equalsIgnoreCase(request.getMethod())
                && "/api/orders".equals(request.getRequestURI());
        if (requiresAuth) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bearer token is required to create orders");
                return;
            }
            String token = authorization.substring(7);
            if (!JwtUtil.isValid(token, secret)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }
            request.setAttribute("authenticatedUserId", JwtUtil.getSubject(token));
            request.setAttribute("authenticatedRole", JwtUtil.getRole(token));
        }
        filterChain.doFilter(request, response);
    }
}
