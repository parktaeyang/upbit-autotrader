package com.backend.websocket;

import com.backend.service.UpbitService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

@Component
public class UpbitWebSocketClient {

    private WebSocket webSocket;
    private boolean running = false;

    private final UpbitService upbitService;
    private double lastBuyPrice = 0.0;
    private boolean holding = false;

    public UpbitWebSocketClient(UpbitService upbitService) {
        this.upbitService = upbitService;
    }

    /** 자동매매 시작 */
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

                        // 구독 메시지
                        String subscribeMsg =
                                "[{\"ticket\":\"test\"},{\"type\":\"ticker\",\"codes\":[\"KRW-BTT\"]}]";
                        ws.sendText(subscribeMsg, true);

                        // 첫 메시지 요청
                        ws.request(1);
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                        if (!running) return null;

                        String msg = StandardCharsets.UTF_8.decode(data).toString();
                        System.out.println("RAW (binary): " + msg);

                        double price = extractTradePrice(msg);
                        if (price > 0) {
                            System.out.printf("📡 현재가: %.8f%n", price);

                            // 매수 로직
                            if (!holding) {
                                double krw = upbitService.getBalance("KRW");
                                System.out.printf("💰 KRW 잔액: %.2f%n", krw);

                                if (krw > 5000) {
                                    upbitService.buyMarketOrder("KRW-BTT", krw);
                                    lastBuyPrice = price;
                                    holding = true;
                                    System.out.printf("✅ 매수 체결 (단가 %.8f)%n", price);
                                }
                            }
                            // 매도 로직
                            else if (holding && price >= lastBuyPrice * 1.01) {
                                double btt = upbitService.getBalance("BTT");
                                System.out.printf("🪙 BTT 잔액: %.8f%n", btt);

                                if (btt > 0) {
                                    upbitService.sellMarketOrder("KRW-BTT", btt);
                                    holding = false;
                                    System.out.printf("✅ 매도 체결 (단가 %.8f)%n", price);
                                }
                            }
                        }

                        // 다음 메시지 요청
                        ws.request(1);
                        return WebSocket.Listener.super.onBinary(ws, data, last);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        // 업비트가 보낸 게 텍스트라면 여기서 처리
                        if (!running) return null;

                        String msg = data.toString();
                        System.out.println("RAW (text): " + msg);

                        ws.request(1);
                        return WebSocket.Listener.super.onText(ws, data, last);
                    }
                });
    }

    /** 자동매매 중지 */
    public void disconnect() {
        if (webSocket != null) {
            running = false;
            webSocket.abort();
            webSocket = null;
            System.out.println("🛑 WebSocket 연결 종료됨 (자동매매 중지)");
        } else {
            System.out.println("⚠️ 자동매매가 실행 중이 아닙니다.");
        }
    }

    /** 현재 상태 확인 */
    public boolean isRunning() {
        return running;
    }

    /** 시세 JSON에서 trade_price 추출 */
    private double extractTradePrice(String msg) {
        String key = "\"trade_price\":";
        int idx = msg.indexOf(key);
        if (idx > 0) {
            int start = idx + key.length();
            int end = msg.indexOf(",", start);
            try {
                return Double.parseDouble(msg.substring(start, end));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}