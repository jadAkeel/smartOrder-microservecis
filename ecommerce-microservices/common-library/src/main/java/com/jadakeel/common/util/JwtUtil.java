package com.jadakeel.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class JwtUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static boolean isValid(String token, String secret) {
        try {
            if (token == null || secret == null || secret.isBlank()) {
                return false;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            Map<String, Object> header = extractJson(parts[0]);
            if (!"HS256".equals(header.get("alg"))) {
                return false;
            }
            String body = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(body, secret).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                return false;
            }
            Map<String, Object> claims = extractClaims(token);
            Number exp = (Number) claims.get("exp");
            return exp != null && Instant.now().getEpochSecond() < exp.longValue()
                    && claims.get("sub") != null
                    && UUID.fromString(claims.get("sub").toString()) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Map<String, Object> extractClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            return extractJson(parts[1]);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not extract claims from token", ex);
        }
    }

    public static String getSubject(String token) {
        Map<String, Object> claims = extractClaims(token);
        Object sub = claims.get("sub");
        return sub != null ? sub.toString() : null;
    }

    public static String getRole(String token) {
        Map<String, Object> claims = extractClaims(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }

    private static String sign(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, Object> extractJson(String encodedJson) throws Exception {
        String json = new String(Base64.getUrlDecoder().decode(encodedJson), StandardCharsets.UTF_8);
        return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
