package com.backend.util;

import java.util.List;

/**
 * RSI (Relative Strength Index) 계산 유틸리티
 * 
 * RSI 공식:
 * - RS = 평균 상승폭 / 평균 하락폭
 * - RSI = 100 - (100 / (1 + RS))
 * 
 * 일반적으로 14기간을 사용하며, 초기 평균은 단순 평균, 이후는 지수 이동 평균을 사용
 */
public class RsiCalculator {
    
    private static final int DEFAULT_PERIOD = 14;
    
    /**
     * 캔들 데이터 리스트로부터 RSI 계산
     * 
     * @param prices 종가 리스트 (최신순, 즉 첫 번째가 가장 최근)
     * @return RSI 값 (0-100)
     */
    public static double calculateRsi(List<Double> prices) {
        return calculateRsi(prices, DEFAULT_PERIOD);
    }
    
    /**
     * 지정된 기간으로 RSI 계산
     * 
     * @param prices 종가 리스트 (최신순)
     * @param period RSI 계산 기간 (일반적으로 14)
     * @return RSI 값 (0-100)
     */
    public static double calculateRsi(List<Double> prices, int period) {
        if (prices == null || prices.size() < period + 1) {
            throw new IllegalArgumentException(
                "RSI 계산을 위해서는 최소 " + (period + 1) + "개의 가격 데이터가 필요합니다. 현재: " + 
                (prices == null ? 0 : prices.size())
            );
        }
        
        // 초기 평균 상승폭/하락폭 계산 (첫 period 개의 변화량 사용)
        double avgGain = 0.0;
        double avgLoss = 0.0;
        
        for (int i = 0; i < period; i++) {
            double change = prices.get(i) - prices.get(i + 1); // 최신 - 이전
            if (change > 0) {
                avgGain += change;
            } else {
                avgLoss += Math.abs(change);
            }
        }
        
        avgGain /= period;
        avgLoss /= period;
        
        // 나머지 데이터로 지수 이동 평균 계산
        for (int i = period; i < prices.size() - 1; i++) {
            double change = prices.get(i) - prices.get(i + 1);
            double gain = change > 0 ? change : 0.0;
            double loss = change < 0 ? Math.abs(change) : 0.0;
            
            // Wilder's Smoothing Method (지수 이동 평균)
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }
        
        // RSI 계산
        if (avgLoss == 0.0) {
            return 100.0; // 하락이 없으면 RSI = 100
        }
        
        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));
        
        return rsi;
    }
}

