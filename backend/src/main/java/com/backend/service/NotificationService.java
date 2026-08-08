package com.backend.service;

import com.backend.dto.TradeNotification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 매매 알림 저장 및 조회. WebSocket 자동매매 로직과 수동 주문(UpbitService) 양쪽에서 공유한다.
 */
@Component
public class NotificationService {

    private static final int MAX_NOTIFICATIONS = 200;

    private final List<TradeNotification> notifications = Collections.synchronizedList(new ArrayList<>());
    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationService(SseEmitterRegistry sseEmitterRegistry) {
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    public void add(String message, String type, String market) {
        TradeNotification notification = new TradeNotification(message, type, market);
        synchronized (notifications) {
            notifications.add(0, notification); // 최신 알림을 앞에 추가
            if (notifications.size() > MAX_NOTIFICATIONS) {
                notifications.remove(notifications.size() - 1);
            }
        }
        System.out.println(message);
        sseEmitterRegistry.broadcastNotification(notification);
    }

    public List<TradeNotification> getAll() {
        synchronized (notifications) {
            return new ArrayList<>(notifications);
        }
    }
}
