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

    // 테스트 모드 여부 (true면 실제 주문 안 날림)
    private final boolean testMode = false;

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

    // 시장가 매수
    public void buyMarketOrder(String market, double krwAmount) {
        if (testMode) {
            System.out.println("💡 [TEST MODE] 매수 시뮬레이션: " + market + " KRW=" + krwAmount);
            return;
        }

        Map<String, String> body = Map.of(
                "market", market,
                "side", "bid",
                "price", String.valueOf(krwAmount),
                "ord_type", "price"
        );

        String jwt = jwtProvider.createJwt();

        String response = webClient.post()
                .uri("/v1/orders")
                .header("Authorization", "Bearer " + jwt)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("✅ [REAL TRADE] 💸매수 응답: " + response);
    }

    // 시장가 매도
    public void sellMarketOrder(String market, double volume) {
        if (testMode) {
            System.out.println("💡 [TEST MODE] 매도 시뮬레이션: " + market + " 수량=" + volume);
            return;
        }

        Map<String, String> body = Map.of(
                "market", market,
                "side", "ask",
                "volume", String.valueOf(volume),
                "ord_type", "market"
        );

        String jwt = jwtProvider.createJwt();

        String response = webClient.post()
                .uri("/v1/orders")
                .header("Authorization", "Bearer " + jwt)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("✅ [REAL TRADE] 🪙매도 응답: " + response);
    }
}
