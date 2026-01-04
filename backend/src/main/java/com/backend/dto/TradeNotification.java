package com.backend.dto;

import java.time.LocalDateTime;

public class TradeNotification {
    private String message;
    private String type; // "BUY", "SELL", "INFO", "WARNING", "ERROR"
    private LocalDateTime timestamp;
    private String market;

    public TradeNotification() {
    }

    public TradeNotification(String message, String type, String market) {
        this.message = message;
        this.type = type;
        this.market = market;
        this.timestamp = LocalDateTime.now();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }
}

