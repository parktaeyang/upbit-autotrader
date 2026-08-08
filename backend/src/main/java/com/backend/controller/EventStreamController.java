package com.backend.controller;

import com.backend.service.SseEmitterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/upbit")
public class EventStreamController {

    private final SseEmitterRegistry sseEmitterRegistry;

    public EventStreamController(SseEmitterRegistry sseEmitterRegistry) {
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    // 알림/가격/상태 실시간 스트림 (notification / price / status 이벤트)
    @GetMapping("/events")
    public SseEmitter events() {
        return sseEmitterRegistry.subscribe();
    }
}
