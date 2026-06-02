package com.jadakeel.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jadakeel.auth.model.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

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
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String body = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(body).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            Map<?, ?> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            Number exp = (Number) payload.get("exp");
            return exp != null && Instant.now().getEpochSecond() < exp.longValue()
                    && payload.get("sub") != null
                    && UUID.fromString(payload.get("sub").toString()) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private String encodeJson(Map<String, ?> value) throws Exception {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
