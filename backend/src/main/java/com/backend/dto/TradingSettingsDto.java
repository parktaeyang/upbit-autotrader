package com.backend.dto;

import java.util.List;

/**
 * 런타임으로 조회/변경 가능한 자동매매 설정 스냅샷.
 */
public record TradingSettingsDto(
        List<String> markets,
        double rsiOversold,
        double rsiOverbought,
        int rsiPeriod,
        int candleMinutes,
        int candleCount,
        long rsiCheckCooldownMs,
        int minOrderKrw
) {
}
