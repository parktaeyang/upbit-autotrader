package com.backend.service;

import com.backend.dto.AccountDto;
import com.backend.util.UpbitJwtProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UpbitService {

    private final WebClient webClient = WebClient.create("https://api.upbit.com");
    private final UpbitJwtProvider jwtProvider;

    public UpbitService(com.backend.config.UpbitProperties props) {
        this.jwtProvider = new UpbitJwtProvider(props.getAccessKey(), props.getSecretKey());
    }

    /**
     * 현재 계좌 조회
     */
    public List<AccountDto> getAccounts() {
        String jwt = jwtProvider.createJwt();

        AccountDto[] response = webClient.get()
                .uri("/v1/accounts")
                .header("Authorization", "Bearer " + jwt)
                .retrieve()
                .bodyToMono(AccountDto[].class)
                .block();

        return Arrays.asList(response);
    }

    public double getBalance(String currency) {
        return getAccounts().stream()
                .filter(acc -> acc.getCurrency().equalsIgnoreCase(currency))
                .findFirst()
                .map(AccountDto::getBalanceAsDouble)
                .orElse(0.0);
    }
        /**
         * 시장가 매수 (KRW로 지정)
         */
    public void buyMarketOrder(String market, double krwAmount) {
        Map<String, String> body = new HashMap<>();
        body.put("market", market);
        body.put("side", "bid");
        body.put("price", String.valueOf(krwAmount));
        body.put("ord_type", "price"); // KRW 전액 매수

        String jwt = jwtProvider.createJwt();

        String response = webClient.post()
                .uri("/v1/orders")
                .header("Authorization", "Bearer " + jwt)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("✅ 매수 응답: " + response);
    }

    /**
     * 시장가 매도 (보유 수량 지정)
     */
    public void sellMarketOrder(String market, double volume) {
        Map<String, String> body = new HashMap<>();
        body.put("market", market);
        body.put("side", "ask");
        body.put("volume", String.valueOf(volume));
        body.put("ord_type", "market");

        String jwt = jwtProvider.createJwt();

        String response = webClient.post()
                .uri("/v1/orders")
                .header("Authorization", "Bearer " + jwt)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("✅ 매도 응답: " + response);
    }
}
