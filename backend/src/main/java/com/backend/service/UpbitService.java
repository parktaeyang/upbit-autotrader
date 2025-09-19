package com.backend.service;

import com.backend.dto.AccountDto;
import com.backend.util.UpbitJwtProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

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

        long krwInt = BigDecimal.valueOf(krwAmount * 0.995)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();

        String priceStr = String.valueOf(krwInt);

        String queryString = "market=" + market +
                "&side=bid" +
                "&price=" + priceStr +
                "&ord_type=price";

        String jwt = jwtProvider.createJwtWithQuery(queryString);

        System.out.println("📤 매수 요청: " + queryString);

        try {
            String response = webClient.post()
                    .uri("/v1/orders")
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("market", market)
                            .with("side", "bid")
                            .with("price", priceStr)
                            .with("ord_type", "price"))
                    .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
                    .block();

            System.out.println("📥 매수 응답: " + response);
        } catch (Exception e) {
            System.err.println("❌ 매수 요청 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 여러 종목에 잔액을 분배해서 시장가 매수
    public void buyMarketOrders(List<String> markets) {
        if (markets == null || markets.isEmpty()) {
            System.out.println("⚠️ 매수할 종목이 없습니다.");
            return;
        }

        double balance = getBalance("KRW");
        if (balance <= 0) {
            System.out.println("⚠️ KRW 잔액이 부족합니다.");
            return;
        }

        // 수수료 감안해 99.5% 사용
        double usableBalance = balance * 0.995;

        // 종목 수만큼 균등 분배
        double perMarket = usableBalance / markets.size();

        System.out.println("💰 총 잔액: " + balance + " KRW");
        System.out.println("📊 종목별 매수금액: " + perMarket + " KRW");

        for (String market : markets) {
            buyMarketOrder(market, perMarket);
        }
    }

    // 시장가 매도
    public void sellMarketOrder(String market, double volume) {
        if (testMode) {
            System.out.println("💡 [TEST MODE] 매도 시뮬레이션: " + market + " 수량=" + volume);
            return;
        }

        String queryString = "market=" + market +
                "&side=ask" +
                "&volume=" + volume +
                "&ord_type=market";

        // 2. JWT 생성 (query_hash 포함)
        String jwt = jwtProvider.createJwtWithQuery(queryString);

        System.out.println("📤 매도 요청: " + queryString);

        try {
            String response = webClient.post()
                    .uri("/v1/orders")
                    .header("Authorization", "Bearer " + jwt)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("market", market)
                            .with("side", "ask")
                            .with("volume", String.valueOf(volume))
                            .with("ord_type", "market"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("📥 매도 응답: " + response);
        } catch (Exception e) {
            System.err.println("❌ 매도 요청 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
