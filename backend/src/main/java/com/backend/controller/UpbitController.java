package com.backend.controller;

import com.backend.config.TradingSettings;
import com.backend.service.NotificationService;
import com.backend.service.UpbitApiClient;
import com.backend.service.UpbitService;
import com.backend.websocket.UpbitWebSocketClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/upbit", produces = MediaType.APPLICATION_JSON_VALUE)
public class UpbitController {

    private final UpbitApiClient upbitApiClient;
    private final UpbitWebSocketClient webSocketClient;
    private final UpbitService upbitService;
    private final TradingSettings tradingSettings;
    private final NotificationService notificationService;

    public UpbitController(UpbitApiClient upbitApiClient,
                           UpbitWebSocketClient webSocketClient,
                           UpbitService upbitService,
                           TradingSettings tradingSettings,
                           NotificationService notificationService) {
        this.upbitApiClient = upbitApiClient;
        this.webSocketClient = webSocketClient;
        this.upbitService = upbitService;
        this.tradingSettings = tradingSettings;
        this.notificationService = notificationService;
    }

    @GetMapping("/accounts")
    public Mono<String> getAccounts() {
        return upbitApiClient.getAccounts();
    }

    @PostMapping("/orders")
    public String placeOrder() {
        //  균등 분배 매수 실행
        var markets = tradingSettings.current().markets();
        upbitService.buyMarketOrders(markets);

        return "🚀 자동매매 분배 (대상 종목: " + markets + ")";
    }

    // 자동매매 시작
    @PostMapping("/auto/start")
    public String startAutoTrading() {
        //  WebSocket 연결로 실시간 모니터링 + 매도/재매수 진행
        webSocketClient.connect(tradingSettings.current().markets());

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

    // 현재 가격 정보 조회
    @GetMapping("/prices")
    public java.util.Map<String, Double> getCurrentPrices() {
        return webSocketClient.getCurrentPrices();
    }

    // 매매 알림 조회
    @GetMapping("/notifications")
    public java.util.List<com.backend.dto.TradeNotification> getNotifications() {
        return notificationService.getAll();
    }
}


