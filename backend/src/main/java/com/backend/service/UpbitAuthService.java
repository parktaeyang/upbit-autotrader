package com.backend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.backend.config.UpbitProperties;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class UpbitAuthService {

    private static final Logger logger = LoggerFactory.getLogger(UpbitAuthService.class);
    private final UpbitProperties upbitProperties;

    public UpbitAuthService(UpbitProperties upbitProperties) {
        this.upbitProperties = upbitProperties;
        validateProperties();
    }

    private void validateProperties() {
        if (upbitProperties.getAccessKey() == null || upbitProperties.getAccessKey().trim().isEmpty()) {
            logger.error("❌ UPBIT_ACCESS_KEY가 설정되지 않았습니다. 환경 변수를 확인하세요.");
            throw new IllegalStateException("UPBIT_ACCESS_KEY가 설정되지 않았습니다.");
        }
        if (upbitProperties.getSecretKey() == null || upbitProperties.getSecretKey().trim().isEmpty()) {
            logger.error("❌ UPBIT_SECRET_KEY가 설정되지 않았습니다. 환경 변수를 확인하세요.");
            throw new IllegalStateException("UPBIT_SECRET_KEY가 설정되지 않았습니다.");
        }
        logger.info("✅ 업비트 API 키가 설정되었습니다. Access Key: {}...", 
                upbitProperties.getAccessKey().substring(0, Math.min(8, upbitProperties.getAccessKey().length())));
    }

    public String buildAuthorizationHeader(Map<String, String> params) {
        String jwtToken = createJwt(params);
        logger.debug("🔑 생성된 JWT 토큰: {}...", jwtToken.substring(0, Math.min(50, jwtToken.length())));
        return "Bearer " + jwtToken;
    }

    private String createJwt(Map<String, String> params) {
        // Secret Key를 명시적으로 바이트 배열로 변환 (업비트 API 요구사항)
        byte[] secretKeyBytes = upbitProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        Algorithm algorithm = Algorithm.HMAC256(secretKeyBytes);

        String nonce = UUID.randomUUID().toString();
        com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withClaim("access_key", upbitProperties.getAccessKey())
                .withClaim("nonce", nonce);

        if (params != null && !params.isEmpty()) {
            String queryString = buildQueryString(params);
            String queryHash = DigestUtils.sha512Hex(queryString);
            builder.withClaim("query_hash", queryHash)
                    .withClaim("query_hash_alg", "SHA512");
            logger.debug("📝 Query String: {}, Query Hash: {}", queryString, queryHash);
        } else {
            logger.debug("📝 Query 파라미터 없음 (GET /v1/accounts)");
        }

        String jwt = builder.sign(algorithm);
        logger.debug("✅ JWT 생성 완료. Nonce: {}", nonce);
        return jwt;
    }

    public String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        SortedMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!first) sb.append('&');
            first = false;
            sb.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return sb.toString();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to URL encode", e);
        }
    }
}


