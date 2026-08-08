package com.backend.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RsiCalculatorTest {

    @Test
    void 지속상승이면_RSI는_100이다() {
        // 과거→현재: 10,11,12,13,14,15 (최신순 리스트로 전달)
        List<Double> pricesNewestFirst = List.of(15.0, 14.0, 13.0, 12.0, 11.0, 10.0);

        double rsi = RsiCalculator.calculateRsi(pricesNewestFirst, 3);

        assertEquals(100.0, rsi, 1e-9);
    }

    @Test
    void 지속하락이면_RSI는_0이다() {
        // 과거→현재: 15,14,13,12,11,10 (최신순 리스트로 전달)
        List<Double> pricesNewestFirst = List.of(10.0, 11.0, 12.0, 13.0, 14.0, 15.0);

        double rsi = RsiCalculator.calculateRsi(pricesNewestFirst, 3);

        assertEquals(0.0, rsi, 1e-9);
    }

    @Test
    void Wilder_평활이_과거에서_현재_순서로_적용된다() {
        // 과거→현재 가격: 10, 12, 11, 15, 14, 20
        // 변화량(과거→현재 순): +2, -1, +4, -1, +6
        // period=2 → 초기평균(가장 오래된 2개: +2,-1): avgGain=1.0, avgLoss=0.5
        // 이후 +4, -1, +6을 시간 순방향으로 Wilder 평활 적용하면
        // avgGain=3.625, avgLoss=0.3125 → RS=11.6 → RSI=100-100/12.6=92.063492...
        List<Double> pricesNewestFirst = List.of(20.0, 14.0, 15.0, 11.0, 12.0, 10.0);

        double rsi = RsiCalculator.calculateRsi(pricesNewestFirst, 2);

        assertEquals(92.06349206349206, rsi, 1e-9);
    }

    @Test
    void 데이터가_부족하면_예외를_던진다() {
        List<Double> tooFew = List.of(10.0, 11.0, 12.0);

        assertThrows(IllegalArgumentException.class, () -> RsiCalculator.calculateRsi(tooFew, 3));
    }
}
