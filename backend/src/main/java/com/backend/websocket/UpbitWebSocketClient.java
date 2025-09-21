package com.backend.websocket;

import com.backend.service.UpbitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UpbitWebSocketClient {

    private final UpbitService upbitService;
    private WebSocket webSocket;

    // 마지막 매수 단가 저장 (market → price)
    private final Map<String, Double> lastBuyPrices = new ConcurrentHashMap<>();
    private final Set<String> markets = new HashSet<>();

    private volatile long lastMessageTime = 0;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpbitWebSocketClient(UpbitService upbitService) {
        this.upbitService = upbitService;
    }

    /**
     * 자동매매 시작 (WebSocket 연결 + lastBuyPrices 초기화)
     */
    public void connect(Collection<String> marketList) {
        markets.clear();
        markets.addAll(marketList);

        System.out.println("🚀 자동매매 대상: " + markets);

        // 1) 보유 코인 기준으로 lastBuyPrices 초기화
        syncLastBuyPrices();

        // 2) WebSocket 연결
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://api.upbit.com/websocket/v1"), new Listener())
                .thenAccept(ws -> {
                    this.webSocket = ws;

                    // 구독 메시지 전송
                    String ticket = UUID.randomUUID().toString();
                    String codes = String.join("\",\"", markets);
                    String msg = "[{\"ticket\":\"" + ticket + "\"},{\"type\":\"ticker\",\"codes\":[\"" + codes + "\"]}]";
                    ws.sendText(msg, true);

                    lastMessageTime = System.currentTimeMillis();
                    System.out.println("✅ WebSocket 연결됨 (자동매매 시작)");
                });

        // 3) heartbeat 모니터링
        scheduler.scheduleAtFixedRate(this::checkHeartbeat, 15, 15, TimeUnit.SECONDS);
    }

    /**
     * 자동매매 중지
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.abort();
            webSocket = null;
            System.out.println("🛑 자동매매 중지 (WebSocket 종료)");
        }
    }

    /**
     * 현재 상태 확인
     */
    public String status() {
        return (webSocket != null)
                ? "✅ 자동매매 실행 중 (대상: " + markets + ")"
                : "⏸ 자동매매 중지됨";
    }

    /**
     * 업비트 계정 조회 API로 lastBuyPrices 초기화
     */
    private void syncLastBuyPrices() {
        try {
            var accounts = upbitService.getAccounts();
            lastBuyPrices.clear();

            accounts.forEach(acc -> {
                String currency = acc.getCurrency(); // 예: BTC, ETH
                String market = "KRW-" + currency;

                try {
                    double balance = Double.parseDouble(acc.getBalance());
                    double avgBuyPrice = Double.parseDouble(acc.getAvgBuyPrice());

                    if (markets.contains(market) && balance > 0) {
                        lastBuyPrices.put(market, avgBuyPrice);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ AccountDto 숫자 변환 실패: " + acc);
                }
            });

            System.out.println("🔄 lastBuyPrices 동기화 완료: " + lastBuyPrices);

        } catch (Exception e) {
            System.err.println("❌ lastBuyPrices 동기화 실패: " + e.getMessage());
        }
    }

    /**
     * WebSocket 연결 유지 확인
     */
    private void checkHeartbeat() {
        long now = System.currentTimeMillis();
        if (lastMessageTime > 0 && now - lastMessageTime > 15000) {
            System.out.println("⚠️ 데이터 수신 끊김 → 재연결 시도");
            reconnect();
        }
    }

    /**
     * 재연결 로직
     */
    private void reconnect() {
        disconnect();
        connect(markets);
    }

    /**
     * WebSocket Listener
     */
    private class Listener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            try {
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                String json = new String(bytes);

                lastMessageTime = System.currentTimeMillis();

                JsonNode obj = objectMapper.readTree(json);
                String type = obj.path("type").asText();
                if ("ticker".equals(type)) {
                    String market = obj.path("code").asText();
                    double tradePrice = obj.path("trade_price").asDouble();

                    System.out.println("📡 현재가 (" + market + "): " + tradePrice);

                    Double lastBuyPrice = lastBuyPrices.get(market);
                    if (lastBuyPrice == null) {
                        // 처음 매수 → 잔액 분배 매수
                        double krwBalance = upbitService.getBalance("KRW");
                        if (krwBalance > 1000) {
                            upbitService.buyMarketOrder(market, krwBalance / markets.size());
                            lastBuyPrices.put(market, tradePrice);
                            System.out.println("✅ 초기 매수 완료: " + market + " @ " + tradePrice);
                        }
                    } else {
                        // 매수 후 1% 상승 시 매도
                        double targetPrice = lastBuyPrice * 1.01;
                        if (tradePrice >= targetPrice) {
                            double volume = upbitService.getBalance(market.split("-")[1]);
                            if (volume > 0) {
                                upbitService.sellMarketOrder(market, volume);
                                lastBuyPrices.remove(market);
                                System.out.println("💰 매도 완료: " + market + " 수익 실현!");
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ onBinary 처리 오류: " + e.getMessage());
            }
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("🔌 WebSocket 종료 (" + statusCode + "): " + reason);
            reconnect();
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("❌ WebSocket 오류: " + error.getMessage());
            reconnect();
        }
    }

}