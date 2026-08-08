package com.backend.service;

import com.backend.dto.TradeNotification;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 연결된 SSE 클라이언트(EventSource)를 관리하고 알림/가격/상태 이벤트를 브로드캐스트한다.
 */
@Component
public class SseEmitterRegistry {

    private static final long HEARTBEAT_INTERVAL_SECONDS = 20L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(this::heartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    private void shutdown() {
        scheduler.shutdownNow();
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음 (하트비트 + 클라이언트 재연결로 유지)
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    public void broadcastNotification(TradeNotification notification) {
        broadcast("notification", notification);
    }

    public void broadcastPrice(String market, double price) {
        broadcast("price", new PriceEvent(market, price));
    }

    public void broadcastStatus(String status) {
        broadcast("status", status);
    }

    private void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }

    private void broadcast(String eventName, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }

    public record PriceEvent(String market, double price) {
    }
}
