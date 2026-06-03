package com.jadakeel.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadakeel.auth.model.UserAccount;
import com.jadakeel.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class JwtService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String secret;
    private final long ttlSeconds;

    public JwtService(@Value("${auth.jwt.secret}") String secret,
                      @Value("${auth.jwt.ttl-seconds}") long ttlSeconds) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String generateToken(UserAccount user) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", user.getId().toString(),
                    "email", user.getEmail(),
                    "role", user.getRole().name(),
                    "exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond()
            ));
            String body = header + "." + payload;
            return body + "." + sign(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate token", ex);
        }
    }

    public boolean isValid(String token) {
        return JwtUtil.isValid(token, secret);
    }

    private String encodeJson(Map<String, ?> value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String body) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
