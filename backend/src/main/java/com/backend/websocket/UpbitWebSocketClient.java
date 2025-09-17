package com.backend.websocket;

import com.backend.service.UpbitService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

@Component
public class UpbitWebSocketClient {
    private final UpbitService upbitService;

    private double lastBuyPrice = 0.0;
    private boolean holding = false;

    public UpbitWebSocketClient(UpbitService upbitService) {
        this.upbitService = upbitService;
    }

    @PostConstruct
    public void connect() {
        HttpClient client = HttpClient.newHttpClient();

        client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://api.upbit.com/websocket/v1"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        System.out.println("✅ WebSocket 연결됨");
                        String subscribeMsg = "[{\"ticket\":\"test\"},{\"type\":\"ticker\",\"codes\":[\"KRW-BTT\"]}]";
                        webSocket.sendText(subscribeMsg, true);
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        String msg = data.toString();
                        double price = extractTradePrice(msg);
                        System.out.println("📡 현재가: " + price);

                        if (!holding) {
                            // ✅ KRW 전액으로 매수
                            double krwBalance = upbitService.getBalance("KRW");
                            if (krwBalance > 5000) { // 최소 주문금액(5천원) 체크
                                upbitService.buyMarketOrder("KRW-BTT", krwBalance);
                                lastBuyPrice = price;
                                holding = true;
                            }
                        } else if (holding && price >= lastBuyPrice * 1.01) {
                            // ✅ BTT 전량 매도
                            double bttBalance = upbitService.getBalance("BTT");
                            if (bttBalance > 0) {
                                upbitService.sellMarketOrder("KRW-BTT", bttBalance);
                                holding = false;
                            }
                        }

                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }
                });
    }

    private double extractTradePrice(String msg) {
        // 단순 파싱 예제 (실제는 JSON 라이브러리 사용)
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
