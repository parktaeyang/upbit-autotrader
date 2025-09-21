package com.backend.controller;

import com.backend.service.UpbitApiClient;
import com.backend.service.UpbitService;
import com.backend.websocket.UpbitWebSocketClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(path = "/api/upbit", produces = MediaType.APPLICATION_JSON_VALUE)
public class UpbitController {

    private final UpbitApiClient upbitApiClient;
    private final UpbitWebSocketClient webSocketClient;
    private final UpbitService upbitService;

    public UpbitController(UpbitApiClient upbitApiClient,
                           UpbitWebSocketClient webSocketClient,
                           UpbitService upbitService) {
        this.upbitApiClient = upbitApiClient;
        this.webSocketClient = webSocketClient;
        this.upbitService = upbitService;
    }

    List<String> markets = Arrays.asList("KRW-BTT", "KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-DOGE");

    @GetMapping("/accounts")
    public Mono<String> getAccounts() {
        return upbitApiClient.getAccounts();
    }

    @PostMapping("/orders")
    public String placeOrder() {
        //  균등 분배 매수 실행
        upbitService.buyMarketOrders(markets);

        return "🚀 자동매매 뿐배 (대상 종목: " + markets + ")";
    }

    // 자동매매 시작
    @PostMapping("/auto/start")
    public String startAutoTrading() {
        //  WebSocket 연결로 실시간 모니터링 + 매도/재매수 진행
        webSocketClient.connect(markets);

        return "🚀 자동매매 시작";
    }

    // 자동매매 중지
    @PostMapping("/auto/stop")
    public String stopAutoTrading() {
        webSocketClient.disconnect();
        return "🛑 자동매매 중지!";
    }

    // 자동매매 상태 확인
    @GetMapping("/auto/status")
    public String getAutoTradingStatus() {
        return webSocketClient.status();
    }
}


