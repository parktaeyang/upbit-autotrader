package com.backend.websocket;

import com.backend.service.UpbitService;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

@Component
public class UpbitWebSocketClient {
    private WebSocket webSocket;  // 연결 객체
    @Getter
    private boolean running = false;

    private final UpbitService upbitService;
    private double lastBuyPrice = 0.0;
    private boolean holding = false;

    public UpbitWebSocketClient(UpbitService upbitService) {
        this.upbitService = upbitService;
    }

    // 자동매매 시작
    public void connect() {
        if (running) {
            System.out.println("⚠️ 이미 자동매매 실행 중입니다.");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://api.upbit.com/websocket/v1"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket ws) {
                        webSocket = ws;
                        running = true;
                        System.out.println("✅ WebSocket 연결됨 (자동매매 시작)");
                        String subscribeMsg = "[{\"ticket\":\"test\"},{\"type\":\"ticker\",\"codes\":[\"KRW-BTT\"]}]";
                        ws.sendText(subscribeMsg, true);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        if (!running) return null;

                        String msg = data.toString();
                        double price = extractTradePrice(msg);
                        System.out.println("📡 현재가: " + price);

                        // 매수/매도 로직
                        if (!holding) {
                            double krw = upbitService.getBalance("KRW");
                            if (krw > 5000) {
                                upbitService.buyMarketOrder("KRW-BTT", krw);
                                lastBuyPrice = price;
                                holding = true;
                            }
                        } else if (holding && price >= lastBuyPrice * 1.01) {
                            double btt = upbitService.getBalance("BTT");
                            if (btt > 0) {
                                upbitService.sellMarketOrder("KRW-BTT", btt);
                                holding = false;
                            }
                        }
                        return WebSocket.Listener.super.onText(ws, data, last);
                    }
                });
    }

    // 자동매매 중지
    public void disconnect() {
        if (webSocket != null) {
            running = false;
            webSocket.abort();  // 즉시 연결 종료
            webSocket = null;
            System.out.println("🛑 WebSocket 연결 종료됨 (자동매매 중지)");
        } else {
            System.out.println("⚠️ 자동매매가 실행 중이 아닙니다.");
        }
    }

    private double extractTradePrice(String msg) {
        String key = "\"trade_price\":";
        int idx = msg.indexOf(key);
        if (idx > 0) {
            int start = idx + key.length();
            int end = msg.indexOf(",", start);
            return Double.parseDouble(msg.substring(start, end));
        }
        return 0.0;
    }

}