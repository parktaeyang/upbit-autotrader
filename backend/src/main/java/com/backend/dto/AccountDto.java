package com.backend.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountDto {

    private String currency;   // 화폐 단위 (KRW, BTC, BTT 등)
    private String balance;    // 사용 가능 잔고
    private String locked;     // 주문 중 묶여있는 금액/코인
    @JsonProperty("avg_buy_price")
    private String avgBuyPrice; // 매수평균가

    // getter/setter
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    public String getBalance() {
        return balance;
    }
    public void setBalance(String balance) {
        this.balance = balance;
    }
    public String getLocked() {
        return locked;
    }
    public void setLocked(String locked) {
        this.locked = locked;
    }
    public String getAvgBuyPrice() {
        return avgBuyPrice;
    }
    public void setAvgBuyPrice(String avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
    }

    // double 변환용 헬퍼
    public double getBalanceAsDouble() {
        try {
            return Double.parseDouble(balance);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
