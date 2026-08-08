package com.backend.websocket;

import com.backend.config.TradingSettings;
import com.backend.dto.CandleDto;
import com.backend.dto.TradingSettingsDto;
import com.backend.service.NotificationService;
import com.backend.service.SseEmitterRegistry;
import com.backend.service.UpbitService;
import com.backend.util.RsiCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

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
import java.util.stream.Collectors;

@Component
public class UpbitWebSocketClient {

    private final UpbitService upbitService;
    private final TradingSettings tradingSettings;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final NotificationService notificationService;
    private WebSocket webSocket;

    // 마지막 매수 단가 저장 (market → price)
    private final Map<String, Double> lastBuyPrices = new ConcurrentHashMap<>();
    private final Set<String> markets = new HashSet<>();

    // 현재가 저장 (market → current price)
    private final Map<String, Double> currentPrices = new ConcurrentHashMap<>();

    // 매매 쿨다운 및 제한
    private final Map<String, Long> lastRsiCheckTime = new ConcurrentHashMap<>(); // 마켓별 마지막 RSI 체크 시간
    private final Map<String, Double> lastRsiValue = new ConcurrentHashMap<>(); // 마켓별 마지막 RSI 값

    private volatile long lastMessageTime = 0;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public UpbitWebSocketClient(UpbitService upbitService, TradingSettings tradingSettings,
                                 SseEmitterRegistry sseEmitterRegistry, NotificationService notificationService) {
        this.upbitService = upbitService;
        this.tradingSettings = tradingSettings;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.notificationService = notificationService;
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
                    sseEmitterRegistry.broadcastStatus(status());
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
            sseEmitterRegistry.broadcastStatus(status());
        }
    }

    /**
     * 현재 상태 확인
     */
    public String status() {
        return isRunning()
                ? "✅ 자동매매 실행 중 (대상: " + markets + ")"
                : "⏸ 자동매매 중지됨";
    }

    /**
     * 자동매매 실행 여부
     */
    public boolean isRunning() {
        return webSocket != null;
    }

    /**
     * 현재 가격 정보 반환 (Frontend용)
     */
    public Map<String, Double> getCurrentPrices() {
        return new HashMap<>(currentPrices);
    }

    /**
     * 알림 추가
     */
    private void addNotification(String message, String type, String market) {
        notificationService.add(message, type, market);
    }

    /**
     * 업비트 계정 조회 API로 보유 코인 정보 초기화
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

            System.out.println("🔄 보유 코인 동기화 완료: " + lastBuyPrices);

        } catch (Exception e) {
            System.err.println("❌ 보유 코인 동기화 실패: " + e.getMessage());
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
     * RSI 기반 매매 신호 체크
     * - RSI 30 이하: 과매도 → 매수 신호
     * - RSI 70 이상: 과매수 → 매도 신호
     */
    private void checkRsiAndTrade(String market) {
        long now = System.currentTimeMillis();
        TradingSettingsDto settings = tradingSettings.current();

        // 쿨다운 체크
        Long lastCheck = lastRsiCheckTime.get(market);
        if (lastCheck != null && now - lastCheck < settings.rsiCheckCooldownMs()) {
            return; // 쿨다운 중이면 스킵
        }
        lastRsiCheckTime.put(market, now);

        try {
            // 캔들 데이터 조회
            List<CandleDto> candles = upbitService.getMinuteCandles(market, settings.candleMinutes(), settings.candleCount());
            if (candles.size() < settings.rsiPeriod() + 1) {
                String message = "⚠️ " + market + ": RSI 계산을 위한 충분한 캔들 데이터가 없습니다. (필요: " +
                    (settings.rsiPeriod() + 1) + ", 현재: " + candles.size() + ")";
                addNotification(message, "WARNING", market);
                return;
            }

            // 종가 리스트 추출 (최신순이므로 그대로 사용)
            List<Double> prices = candles.stream()
                    .map(CandleDto::getTradePrice)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // RSI 계산
            double rsi = RsiCalculator.calculateRsi(prices, settings.rsiPeriod());
            lastRsiValue.put(market, rsi);

            String rsiMessage = "📊 " + market + " RSI: " + String.format("%.2f", rsi);
            addNotification(rsiMessage, "INFO", market);

            // 매매 로직
            String currency = market.split("-")[1];
            double balance = upbitService.getBalance(currency);
            double krwBalance = upbitService.getBalance("KRW");

            // 디버깅 정보 출력
            if (rsi <= settings.rsiOversold() || rsi >= settings.rsiOverbought()) {
                String debugMessage = "🔍 " + market + " 상태 - RSI: " + String.format("%.2f", rsi) +
                    ", 보유량: " + balance + ", KRW잔액: " + String.format("%.0f", krwBalance);
                addNotification(debugMessage, "INFO", market);
            }

            // RSI 과매도: 매수 신호
            if (rsi <= settings.rsiOversold()) {
                if (balance == 0) {
                    if (krwBalance > settings.minOrderKrw()) {
                        // 보유하지 않은 경우 매수
                        double buyAmount = krwBalance / markets.size(); // 잔액을 종목 수로 나눔
                        if (buyAmount >= settings.minOrderKrw()) {
                            upbitService.buyMarketOrder(market, buyAmount);
                            String buyMessage = "🟢 매수 신호 (RSI " + String.format("%.2f", rsi) + " ≤ " + settings.rsiOversold() + "): " + market +
                                " - 매수금액: " + String.format("%.0f", buyAmount) + " KRW";
                            addNotification(buyMessage, "BUY", market);
                        } else {
                            String warningMessage = "⚠️ " + market + ": 매수금액이 최소주문금액(" + settings.minOrderKrw() + "원) 미만입니다. (계산된 금액: " +
                                String.format("%.0f", buyAmount) + "원)";
                            addNotification(warningMessage, "WARNING", market);
                        }
                    } else {
                        String warningMessage = "⚠️ " + market + ": KRW 잔액이 부족합니다. (현재: " +
                            String.format("%.0f", krwBalance) + "원, 필요: " + settings.minOrderKrw() + "원 이상)";
                        addNotification(warningMessage, "WARNING", market);
                    }
                } else {
                    String infoMessage = "ℹ️ " + market + ": 이미 보유 중입니다. (보유량: " + balance + ")";
                    addNotification(infoMessage, "INFO", market);
                }
            }
            // RSI 과매수: 매도 신호
            else if (rsi >= settings.rsiOverbought()) {
                if (balance > 0) {
                    // 보유 중인 경우 매도
                    upbitService.sellMarketOrder(market, balance);
                    String sellMessage = "🔴 매도 신호 (RSI " + String.format("%.2f", rsi) + " ≥ " + settings.rsiOverbought() + "): " + market;
                    addNotification(sellMessage, "SELL", market);
                } else {
                    String infoMessage = "ℹ️ " + market + ": 보유하지 않아 매도할 수 없습니다.";
                    addNotification(infoMessage, "INFO", market);
                }
            }

        } catch (Exception e) {
            String errorMessage = "❌ RSI 체크 오류 (" + market + "): " + e.getMessage();
            addNotification(errorMessage, "ERROR", market);
            System.err.println(errorMessage);
            e.printStackTrace();
        }
    }
    /**
     * WebSocket Listener
     */
    private class Listener implements WebSocket.Listener {
        
        // 불완전한 JSON 메시지를 위한 버퍼
        private StringBuilder messageBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            try {
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                String chunk = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                
                // 버퍼에 추가
                messageBuffer.append(chunk);
                
                // 마지막 청크가 아니면 계속 누적
                if (!last) {
                    return WebSocket.Listener.super.onBinary(webSocket, data, last);
                }
                
                // 마지막 청크면 버퍼 전체를 처리
                String fullMessage = messageBuffer.toString();
                messageBuffer.setLength(0); // 버퍼 초기화
                
                lastMessageTime = System.currentTimeMillis();
                
                // 개행 문자로 여러 메시지 분리 (업비트는 여러 티커를 개행으로 구분)
                String[] messages = fullMessage.split("\n");
                
                for (String json : messages) {
                    if (json.trim().isEmpty()) continue;
                    
                    try {
                        processTickerMessage(json.trim());
                    } catch (Exception e) {
                        // 개별 메시지 파싱 실패는 로그만 출력하고 계속 진행
                        System.err.println("⚠️ 티커 메시지 파싱 실패: " + e.getMessage());
                        // 디버깅용: 문제가 되는 메시지 첫 100자만 출력
                        if (json.length() > 100) {
                            System.err.println("  메시지 샘플: " + json.substring(0, 100) + "...");
                        } else {
                            System.err.println("  메시지: " + json);
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("❌ onBinary 처리 오류: " + e.getMessage());
            }
            return WebSocket.Listener.super.onBinary(webSocket, data, last);
        }
        
        /**
         * 티커 메시지 처리
         */
        private void processTickerMessage(String json) throws Exception {
            JsonNode obj = objectMapper.readTree(json);
            String type = obj.path("type").asText();
            if ("ticker".equals(type)) {
                    String market = obj.path("code").asText();
                    double tradePrice = obj.path("trade_price").asDouble();

                    // 이전 가격과 비교하여 1% 이상 변동이 있을 때만 로그 출력
                    Double previousPrice = currentPrices.get(market);
                    if (previousPrice != null) {
                        double changePercent = Math.abs((tradePrice - previousPrice) / previousPrice) * 100;
                        if (changePercent >= 1.0) {
                            System.out.println("📡 현재가 (" + market + "): " + tradePrice + 
                                " (변동: " + String.format("%.2f", changePercent) + "%)");
                        }
                    }

                    // 현재가 갱신
                    currentPrices.put(market, tradePrice);
                    sseEmitterRegistry.broadcastPrice(market, tradePrice);

                    // RSI 기반 매매 신호 체크
                    checkRsiAndTrade(market);
                }
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