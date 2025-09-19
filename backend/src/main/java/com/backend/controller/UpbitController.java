package com.backend.controller;

import com.backend.service.UpbitApiClient;
import com.backend.websocket.UpbitWebSocketClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/upbit", produces = MediaType.APPLICATION_JSON_VALUE)
public class UpbitController {

    private final UpbitApiClient upbitApiClient;
    private final UpbitWebSocketClient webSocketClient;

    public UpbitController(UpbitApiClient upbitApiClient,
                           UpbitWebSocketClient webSocketClient) {
        this.upbitApiClient = upbitApiClient;
        this.webSocketClient = webSocketClient;
    }

    @GetMapping("/accounts")
    public Mono<String> getAccounts() {
        return upbitApiClient.getAccounts();
    }

    @PostMapping("/orders")
    public Mono<String> placeOrder(
            @RequestParam String market,
            @RequestParam String side,
            @RequestParam(required = false) String volume,
            @RequestParam(required = false) String price,
            @RequestParam("ord_type") String ordType
    ) {
        return upbitApiClient.placeOrder(market, side, volume, price, ordType);
    }

    // 자동매매 시작
    @PostMapping("/auto/start")
    public String startAutoTrading() {
        webSocketClient.connect();
        return "🚀 자동매매 시작!";
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
        return webSocketClient.isRunning()
                ? "✅ 자동매매 실행 중"
                : "⏹ 자동매매 중지됨";
    }
}


