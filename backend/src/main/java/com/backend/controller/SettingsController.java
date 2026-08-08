package com.backend.controller;

import com.backend.config.TradingSettings;
import com.backend.dto.TradingSettingsDto;
import com.backend.websocket.UpbitWebSocketClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/upbit/settings", produces = MediaType.APPLICATION_JSON_VALUE)
public class SettingsController {

    private final TradingSettings tradingSettings;
    private final UpbitWebSocketClient webSocketClient;

    public SettingsController(TradingSettings tradingSettings, UpbitWebSocketClient webSocketClient) {
        this.tradingSettings = tradingSettings;
        this.webSocketClient = webSocketClient;
    }

    @GetMapping
    public TradingSettingsDto getSettings() {
        return tradingSettings.current();
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateSettings(@RequestBody TradingSettingsDto dto) {
        if (webSocketClient.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("자동매매 실행 중에는 설정을 변경할 수 없습니다. 먼저 중지하세요.");
        }
        try {
            tradingSettings.update(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(tradingSettings.current());
    }
}
