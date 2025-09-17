package com.backend.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.backend.config.UpbitProperties;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class UpbitAuthService {

    private final UpbitProperties upbitProperties;

    public UpbitAuthService(UpbitProperties upbitProperties) {
        this.upbitProperties = upbitProperties;
    }

    public String buildAuthorizationHeader(Map<String, String> params) {
        String jwtToken = createJwt(params);
        return "Bearer " + jwtToken;
    }

    private String createJwt(Map<String, String> params) {
        Algorithm algorithm = Algorithm.HMAC256(upbitProperties.getSecretKey());

        com.auth0.jwt.JWTCreator.Builder builder = JWT.create()
                .withClaim("access_key", upbitProperties.getAccessKey())
                .withClaim("nonce", UUID.randomUUID().toString());

        if (params != null && !params.isEmpty()) {
            String queryString = buildQueryString(params);
            String queryHash = DigestUtils.sha512Hex(queryString);
            builder.withClaim("query_hash", queryHash)
                    .withClaim("query_hash_alg", "SHA512");
        }

        return builder.sign(algorithm);
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


