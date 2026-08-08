import { useEffect, useState } from "react";
import * as styles from "../styles";
import type { TradingSettings } from "../api/types";

interface Props {
    settings: TradingSettings | null;
    loading: boolean;
    error: string | null;
    running: boolean;
    onSave: (next: TradingSettings) => Promise<boolean>;
}

interface FormState {
    markets: string; // 콤마로 구분된 마켓 코드 입력값
    rsiOversold: string;
    rsiOverbought: string;
    rsiPeriod: string;
    candleMinutes: string;
    candleCount: string;
    rsiCheckCooldownMs: string;
    minOrderKrw: string;
}

function toFormState(settings: TradingSettings): FormState {
    return {
        markets: settings.markets.join(", "),
        rsiOversold: String(settings.rsiOversold),
        rsiOverbought: String(settings.rsiOverbought),
        rsiPeriod: String(settings.rsiPeriod),
        candleMinutes: String(settings.candleMinutes),
        candleCount: String(settings.candleCount),
        rsiCheckCooldownMs: String(settings.rsiCheckCooldownMs),
        minOrderKrw: String(settings.minOrderKrw),
    };
}

export default function SettingsPanel({ settings, loading, error, running, onSave }: Props) {
    const [form, setForm] = useState<FormState | null>(null);
    const [saving, setSaving] = useState(false);
    const [saveMessage, setSaveMessage] = useState<string | null>(null);

    useEffect(() => {
        if (settings) setForm(toFormState(settings));
    }, [settings]);

    const disabled = running || loading || saving || !form;

    const handleChange = (field: keyof FormState) => (e: React.ChangeEvent<HTMLInputElement>) => {
        setForm((prev) => (prev ? { ...prev, [field]: e.target.value } : prev));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!form) return;

        const markets = form.markets
            .split(",")
            .map((m) => m.trim().toUpperCase())
            .filter((m) => m.length > 0);

        const next: TradingSettings = {
            markets,
            rsiOversold: Number(form.rsiOversold),
            rsiOverbought: Number(form.rsiOverbought),
            rsiPeriod: Number(form.rsiPeriod),
            candleMinutes: Number(form.candleMinutes),
            candleCount: Number(form.candleCount),
            rsiCheckCooldownMs: Number(form.rsiCheckCooldownMs),
            minOrderKrw: Number(form.minOrderKrw),
        };

        setSaving(true);
        setSaveMessage(null);
        const ok = await onSave(next);
        setSaveMessage(ok ? "✅ 설정이 저장되었습니다." : null);
        setSaving(false);
    };

    return (
        <section style={styles.section}>
            <h2>자동매매 설정</h2>
            <div style={styles.card}>
                {running && (
                    <p style={{ margin: "0 0 12px", color: "#dc3545" }}>
                        ⚠️ 자동매매 실행 중에는 설정을 변경할 수 없습니다. 먼저 중지하세요.
                    </p>
                )}
                {!form ? (
                    <p style={styles.emptyText}>설정을 불러오는 중...</p>
                ) : (
                    <form onSubmit={handleSubmit}>
                        <div style={{ ...styles.formRow, marginBottom: 12 }}>
                            <label style={styles.formLabel}>매매 대상 마켓 (콤마로 구분, 예: KRW-BTC, KRW-ETH)</label>
                            <input
                                style={styles.formInput}
                                type="text"
                                value={form.markets}
                                disabled={disabled}
                                onChange={handleChange("markets")}
                            />
                        </div>

                        <div style={styles.formGrid}>
                            <div style={styles.formRow}>
                                <label style={styles.formLabel}>RSI 과매도 기준 (매수)</label>
                                <input
                                    style={styles.formInput}
                                    type="number"
                                    value={form.rsiOversold}
                                    disabled={disabled}
                                    onChange={handleChange("rsiOversold")}
                                />
                            </div>
                            <div style={styles.formRow}>
                                <label style={styles.formLabel}>RSI 과매수 기준 (매도)</label>
                                <input
                                    style={styles.formInput}
                                    type="number"
                                    value={form.rsiOverbought}
                                    disabled={disabled}
                                    onChange={handleChange("rsiOverbought")}
                                />
                            </div>
                            <div style={styles.formRow}>
                                <label style={styles.formLabel}>RSI 계산 기간</label>
                                <input
                                    style={styles.formInput}
                                    type="number"
                                    value={form.rsiPeriod}
                                    disabled={disabled}
                                    onChange={handleChange("rsiPeriod")}
                                />
                            </div>
                            <div style={styles.formRow}>
                                <label style={styles.formLabel}>캔들 단위(분)</label>
                                <input
                                    style={styles.formInput}
                                    type="number"
                                    value={form.candleMinutes}
                                    disabled={disabled}
                                    onChange={handleChange("candleMinutes")}
                                />
                            </div>
                            <div style={styles.formRow}>
                                <label style={styles.formLabel}>조회 캔들 개수</label>
                                <input
                                    style={styles.formInput}
                                    type="number"
                                    value={form.candleCount}
                                    disabled={disabled}
                                    onChange={handleChange("candleCount")}
                                />
                            </div>
                            <div style={styles.formRow}>
                                <label style={styles.formLabel}>RSI 체크 쿨다운 (ms)</label>
                                <input
                                    style={styles.formInput}
                                    type="number"
                                    value={form.rsiCheckCooldownMs}
                                    disabled={disabled}
                                    onChange={handleChange("rsiCheckCooldownMs")}
                                />
                            </div>
                            <div style={styles.formRow}>
                                <label style={styles.formLabel}>최소 주문 금액 (KRW)</label>
                                <input
                                    style={styles.formInput}
                                    type="number"
                                    value={form.minOrderKrw}
                                    disabled={disabled}
                                    onChange={handleChange("minOrderKrw")}
                                />
                            </div>
                        </div>

                        <button
                            type="submit"
                            style={{ ...styles.btn, marginTop: 16, background: "#0d6efd" }}
                            disabled={disabled}
                        >
                            {saving ? "저장 중..." : "💾 설정 저장"}
                        </button>

                        {error && <p style={{ color: "#dc3545", marginTop: 8 }}>❌ {error}</p>}
                        {saveMessage && <p style={{ color: "#28a745", marginTop: 8 }}>{saveMessage}</p>}
                    </form>
                )}
            </div>
        </section>
    );
}
