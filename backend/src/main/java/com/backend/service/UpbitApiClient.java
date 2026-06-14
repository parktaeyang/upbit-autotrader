package com.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class UpbitApiClient {

    private static final Logger logger = LoggerFactory.getLogger(UpbitApiClient.class);
    private static final Set<String> HIDDEN_CURRENCIES = Set.of("VTHO", "CHR");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient upbitWebClient;
    private final UpbitAuthService authService;

    public UpbitApiClient(WebClient upbitWebClient, UpbitAuthService authService) {
        this.upbitWebClient = upbitWebClient;
        this.authService = authService;
    }

    public Mono<String> getAccounts() {
        logger.info("📡 업비트 계좌 조회 요청 시작");
        String authorization = authService.buildAuthorizationHeader(null);
        
        return upbitWebClient.get()
                .uri("/v1/accounts")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JsonNode root = objectMapper.readTree(response);
                        ArrayNode filtered = objectMapper.createArrayNode();
                        for (JsonNode node : root) {
                            String currency = node.path("currency").asText();
                            if (!HIDDEN_CURRENCIES.contains(currency.toUpperCase())) {
                                filtered.add(node);
                            }
                        }
                        return objectMapper.writeValueAsString(filtered);
                    } catch (Exception e) {
                        logger.warn("⚠️ 계좌 필터링 실패, 원본 반환: {}", e.getMessage());
                        return response;
                    }
                })
                .doOnSuccess(response -> logger.info("✅ 계좌 조회 성공: {}", response))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        logger.error("❌ 업비트 API 오류 - Status: {}, Body: {}",
                                ex.getStatusCode(), ex.getResponseBodyAsString());
                    } else {
                        logger.error("❌ 계좌 조회 실패: {}", error.getMessage(), error);
                    }
                });
    }

    public Mono<String> placeOrder(String market, String side, String volume, String price, String ordType) {
        Map<String, String> params = new HashMap<>();
        params.put("market", market);
        params.put("side", side);
        if (volume != null) params.put("volume", volume);
        if (price != null) params.put("price", price);
        params.put("ord_type", ordType);

        String authorization = authService.buildAuthorizationHeader(params);

        return upbitWebClient.post()
                .uri("/v1/orders")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(params))
                .retrieve()
                .bodyToMono(String.class);
    }
}


