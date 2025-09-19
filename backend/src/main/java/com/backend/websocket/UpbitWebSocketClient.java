package com.backend.websocket;

import com.backend.service.UpbitService;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class UpbitWebSocketClient {

    private WebSocket webSocket;
    private boolean running = false;

    private final UpbitService upbitService;

    // 종목별 마지막 매수가격
    private final Map<String, Double> lastBuyPrices = new HashMap<>();
    // 보유중인 종목
    private final Set<String> holdings = new HashSet<>();

    // 자동 재연결 관련
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Instant lastMessageTime = Instant.now();

    public UpbitWebSocketClient(UpbitService upbitService) {
        this.upbitService = upbitService;

        // 주기적으로 연결 상태 점검 (15초 이상 메시지 없으면 재연결)
        scheduler.scheduleAtFixedRate(() -> {
            if (running && Instant.now().minusSeconds(15).isAfter(lastMessageTime)) {
                System.out.println("⚠️ 데이터 수신 끊김 → 재연결 시도");
                reconnect();
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    /** 자동매매 시작 */
    public void connect(List<String> markets) {
        if (running) {
            System.out.println("⚠️ 이미 자동매매 실행 중입니다.");
            return;
        }

        String ticket = UUID.randomUUID().toString();
        String codes = markets.stream()
                .map(m -> "\"" + m + "\"")
                .collect(Collectors.joining(","));

        String message = "[{\"ticket\":\"" + ticket + "\"}," +
                "{\"type\":\"ticker\",\"codes\":[" + codes + "]}]";

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://api.upbit.com/websocket/v1"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket ws) {
                        webSocket = ws;
                        running = true;
                        System.out.println("✅ WebSocket 연결됨 (자동매매 시작)");

                        // 구독 메시지 전송
                        ws.sendText(message, true);

                        // 첫 메시지 요청
                        ws.request(1);
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                        if (!running) return null;
                        lastMessageTime = Instant.now();

                        String msg = StandardCharsets.UTF_8.decode(data).toString();
                        String market = extractMarket(msg);
                        double price = extractTradePrice(msg);

                        if (market != null && price > 0) {
                            System.out.printf("📡 [%s] 현재가: %.8f%n", market, price);

                            // 매수 로직
                            if (!holdings.contains(market)) {
                                double krw = upbitService.getBalance("KRW");
                                if (krw > 5000) {
                                    double perMarket = krw / 5; // 5종목 균등 분배
                                    upbitService.buyMarketOrder(market, perMarket);
                                    lastBuyPrices.put(market, price);
                                    holdings.add(market);
                                    System.out.printf("✅ [%s] 매수 체결 (단가 %.8f)%n", market, price);
                                }
                            }
                            // 매도 로직 (1% 상승 시)
                            else if (price >= lastBuyPrices.get(market) * 1.01) {
                                String currency = market.replace("KRW-", ""); // BTC, ETH, BTT, XRP, DOGE
                                double volume = upbitService.getBalance(currency);
                                if (volume > 0) {
                                    upbitService.sellMarketOrder(market, volume);
                                    holdings.remove(market);
                                    System.out.printf("✅ [%s] 매도 체결 (단가 %.8f)%n", market, price);
                                }
                            }
                        }

                        ws.request(1);
                        return WebSocket.Listener.super.onBinary(ws, data, last);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        if (!running) return null;
                        lastMessageTime = Instant.now();
                        System.out.println("RAW (text): " + data);
                        ws.request(1);
                        return WebSocket.Listener.super.onText(ws, data, last);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
                        System.out.println("❌ WebSocket 연결 종료 (" + reason + ")");
                        running = false;
                        reconnect();
                        return WebSocket.Listener.super.onClose(ws, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        System.err.println("❌ WebSocket 오류: " + error.getMessage());
                        running = false;
                        reconnect();
                    }
                });
    }

    /** 자동 재연결 */
    private void reconnect() {
        if (running) return;
        System.out.println("🔄 자동 재연결 시도 중...");
        disconnect();
        // holdings, lastBuyPrices 유지 → 전략 지속
        connect(new ArrayList<>(lastBuyPrices.keySet().isEmpty()
                ? List.of("KRW-BTT", "KRW-BTC", "KRW-ETH", "KRW-XRP", "KRW-DOGE")
                : lastBuyPrices.keySet()));
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

    /** 시세 JSON에서 market 코드 추출 */
    private String extractMarket(String msg) {
        String key = "\"code\":\"";
        int idx = msg.indexOf(key);
        if (idx > 0) {
            int start = idx + key.length();
            int end = msg.indexOf("\"", start);
            return msg.substring(start, end);
        }
        return null;
    }
}