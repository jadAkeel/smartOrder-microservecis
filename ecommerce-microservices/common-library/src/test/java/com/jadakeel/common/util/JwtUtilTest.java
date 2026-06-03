package com.jadakeel.common.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-unit-tests";

    @Test
    void isValid_validToken_returnsTrue() {
        String token = generateToken(UUID.randomUUID().toString(), "CUSTOMER", 3600);
        assertTrue(JwtUtil.isValid(token, SECRET));
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        String token = generateToken(UUID.randomUUID().toString(), "CUSTOMER", -10);
        assertFalse(JwtUtil.isValid(token, SECRET));
    }

    @Test
    void isValid_malformedToken_returnsFalse() {
        assertFalse(JwtUtil.isValid("invalid.token", SECRET));
        assertFalse(JwtUtil.isValid("", SECRET));
        assertFalse(JwtUtil.isValid("a.b.c.d", SECRET));
    }

    @Test
    void isValid_tamperedSignature_returnsFalse() {
        String token = generateToken(UUID.randomUUID().toString(), "ADMIN", 3600);
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".invalidsignature";
        assertFalse(JwtUtil.isValid(tampered, SECRET));
    }

    @Test
    void isValid_rejectsUnsupportedAlgorithm() {
        String token = generateToken(UUID.randomUUID().toString(), "CUSTOMER", 3600, "none");

        assertFalse(JwtUtil.isValid(token, SECRET));
    }

    @Test
    void getSubject_returnsCorrectSubject() {
        String userId = UUID.randomUUID().toString();
        String token = generateToken(userId, "CUSTOMER", 3600);
        assertEquals(userId, JwtUtil.getSubject(token));
    }

    @Test
    void getRole_returnsCorrectRole() {
        String token = generateToken(UUID.randomUUID().toString(), "ADMIN", 3600);
        assertEquals("ADMIN", JwtUtil.getRole(token));
    }

    @Test
    void extractClaims_containsAllFields() {
        String userId = UUID.randomUUID().toString();
        String token = generateToken(userId, "DRIVER", 3600);
        Map<String, Object> claims = JwtUtil.extractClaims(token);
        assertEquals(userId, claims.get("sub"));
        assertEquals("DRIVER", claims.get("role"));
        assertNotNull(claims.get("exp"));
    }

    private String generateToken(String subject, String role, long ttlSeconds) {
        return generateToken(subject, role, ttlSeconds, "HS256");
    }

    private String generateToken(String subject, String role, long ttlSeconds, String algorithm) {
        try {
            String header = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(String.format("{\"alg\":\"%s\",\"typ\":\"JWT\"}", algorithm).getBytes());
            String payloadJson = String.format(
                    "{\"sub\":\"%s\",\"role\":\"%s\",\"exp\":%d}",
                    subject, role, Instant.now().plusSeconds(ttlSeconds).getEpochSecond()
            );
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes());
            String body = header + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return body + "." + signature;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate test token", ex);
        }
    }
}
