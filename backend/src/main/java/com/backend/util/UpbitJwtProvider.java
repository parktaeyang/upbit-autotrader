package com.backend.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpbitJwtProvider {

    private final String accessKey;
    private final String secretKey;

    public UpbitJwtProvider(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * 단순히 uuid만 넣는 경우 (POST /v1/orders 같은 body 없는 요청)
     */
    public String createJwt() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("access_key", accessKey);
        payload.put("nonce", UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(payload)
                .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    /**
     * query_hash를 넣는 경우 (GET 파라미터 있는 요청)
     */
    public String createJwtWithQuery(String queryString) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] queryHash = md.digest(queryString.getBytes(StandardCharsets.UTF_8));
            String queryHashBase16 = bytesToHex(queryHash);

            Map<String, Object> payload = new HashMap<>();
            payload.put("access_key", accessKey);
            payload.put("nonce", UUID.randomUUID().toString());
            payload.put("query_hash", queryHashBase16);
            payload.put("query_hash_alg", "SHA512");

            return Jwts.builder()
                    .setClaims(payload)
                    .signWith(SignatureAlgorithm.HS256, secretKey.getBytes(StandardCharsets.UTF_8))
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("JWT 생성 실패", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
