package com.backend.config;

import com.backend.dto.TradingSettingsDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * 자동매매 대상 마켓 및 RSI 파라미터의 런타임 설정 홀더.
 * 트레이딩 로직(WebSocket 리스너 스레드)과 설정 변경(HTTP 스레드)이
 * 서로 다른 스레드에서 접근하므로 불변 스냅샷 교체 방식을 사용한다.
 */
@Component
public class TradingSettings {

    private static final Pattern MARKET_PATTERN = Pattern.compile("^[A-Z]+-[A-Z0-9]+$");
    private static final Set<Integer> ALLOWED_CANDLE_MINUTES = Set.of(1, 3, 5, 15, 30, 60, 240);

    private static final TradingSettingsDto DEFAULT = new TradingSettingsDto(
            List.of("KRW-BTC", "KRW-ETH", "KRW-XRP"),
            40.0,
            65.0,
            14,
            5,
            30,
            60_000L,
            5000
    );

    private final AtomicReference<TradingSettingsDto> current = new AtomicReference<>(DEFAULT);

    public TradingSettingsDto current() {
        return current.get();
    }

    /**
     * 검증 후 설정을 원자적으로 교체한다.
     *
     * @throws IllegalArgumentException 유효하지 않은 설정인 경우
     */
    public void update(TradingSettingsDto next) {
        validate(next);
        current.set(next);
    }

    private void validate(TradingSettingsDto dto) {
        if (dto.markets() == null || dto.markets().isEmpty()) {
            throw new IllegalArgumentException("매매 대상 마켓 목록은 비어있을 수 없습니다.");
        }
        if (dto.markets().size() != Set.copyOf(dto.markets()).size()) {
            throw new IllegalArgumentException("매매 대상 마켓 목록에 중복된 항목이 있습니다.");
        }
        for (String market : dto.markets()) {
            if (market == null || !MARKET_PATTERN.matcher(market).matches()) {
                throw new IllegalArgumentException("잘못된 마켓 코드 형식입니다: " + market);
            }
        }
        if (!(dto.rsiOversold() > 0 && dto.rsiOversold() < 100)) {
            throw new IllegalArgumentException("rsiOversold는 0과 100 사이여야 합니다.");
        }
        if (!(dto.rsiOverbought() > 0 && dto.rsiOverbought() < 100)) {
            throw new IllegalArgumentException("rsiOverbought는 0과 100 사이여야 합니다.");
        }
        if (dto.rsiOversold() >= dto.rsiOverbought()) {
            throw new IllegalArgumentException("rsiOversold는 rsiOverbought보다 작아야 합니다.");
        }
        if (dto.rsiPeriod() <= 0) {
            throw new IllegalArgumentException("rsiPeriod는 0보다 커야 합니다.");
        }
        if (dto.candleCount() < dto.rsiPeriod() + 1) {
            throw new IllegalArgumentException(
                    "candleCount는 최소 rsiPeriod+1(" + (dto.rsiPeriod() + 1) + ") 이상이어야 합니다.");
        }
        if (!ALLOWED_CANDLE_MINUTES.contains(dto.candleMinutes())) {
            throw new IllegalArgumentException("candleMinutes는 1,3,5,15,30,60,240 중 하나여야 합니다.");
        }
        if (dto.rsiCheckCooldownMs() < 0) {
            throw new IllegalArgumentException("rsiCheckCooldownMs는 0 이상이어야 합니다.");
        }
        if (dto.minOrderKrw() <= 0) {
            throw new IllegalArgumentException("minOrderKrw는 0보다 커야 합니다.");
        }
    }
}
