package com.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 업비트 캔들 데이터 DTO
 */
public class CandleDto {
    
    @JsonProperty("market")
    private String market;
    
    @JsonProperty("candle_date_time_utc")
    private String candleDateTimeUtc;
    
    @JsonProperty("candle_date_time_kst")
    private String candleDateTimeKst;
    
    @JsonProperty("opening_price")
    private Double openingPrice;
    
    @JsonProperty("high_price")
    private Double highPrice;
    
    @JsonProperty("low_price")
    private Double lowPrice;
    
    @JsonProperty("trade_price")
    private Double tradePrice; // 종가
    
    @JsonProperty("candle_acc_trade_volume")
    private Double candleAccTradeVolume;
    
    @JsonProperty("candle_acc_trade_price")
    private Double candleAccTradePrice;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    // Getters and Setters
    public String getMarket() {
        return market;
    }
    
    public void setMarket(String market) {
        this.market = market;
    }
    
    public String getCandleDateTimeUtc() {
        return candleDateTimeUtc;
    }
    
    public void setCandleDateTimeUtc(String candleDateTimeUtc) {
        this.candleDateTimeUtc = candleDateTimeUtc;
    }
    
    public String getCandleDateTimeKst() {
        return candleDateTimeKst;
    }
    
    public void setCandleDateTimeKst(String candleDateTimeKst) {
        this.candleDateTimeKst = candleDateTimeKst;
    }
    
    public Double getOpeningPrice() {
        return openingPrice;
    }
    
    public void setOpeningPrice(Double openingPrice) {
        this.openingPrice = openingPrice;
    }
    
    public Double getHighPrice() {
        return highPrice;
    }
    
    public void setHighPrice(Double highPrice) {
        this.highPrice = highPrice;
    }
    
    public Double getLowPrice() {
        return lowPrice;
    }
    
    public void setLowPrice(Double lowPrice) {
        this.lowPrice = lowPrice;
    }
    
    public Double getTradePrice() {
        return tradePrice;
    }
    
    public void setTradePrice(Double tradePrice) {
        this.tradePrice = tradePrice;
    }
    
    public Double getCandleAccTradeVolume() {
        return candleAccTradeVolume;
    }
    
    public void setCandleAccTradeVolume(Double candleAccTradeVolume) {
        this.candleAccTradeVolume = candleAccTradeVolume;
    }
    
    public Double getCandleAccTradePrice() {
        return candleAccTradePrice;
    }
    
    public void setCandleAccTradePrice(Double candleAccTradePrice) {
        this.candleAccTradePrice = candleAccTradePrice;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}

