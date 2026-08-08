import * as styles from "../styles";

interface Props {
    busy: boolean;
    running: boolean;
    lastAction: string;
    onOrders: () => void;
    onStart: () => void;
    onStop: () => void;
    onAccounts: () => void;
}

export default function ControlPanel({ busy, running, lastAction, onOrders, onStart, onStop, onAccounts }: Props) {
    return (
        <section style={styles.section}>
            <h2>컨트롤</h2>
            <div style={styles.buttons}>
                <button style={styles.btn} disabled={busy} onClick={onOrders}>
                    🛒 균등 분배 매수 (1회)
                </button>
                <button
                    style={{ ...styles.btn, background: "#0d6efd" }}
                    disabled={busy || running}
                    onClick={onStart}
                >
                    ▶️ 자동매매 시작
                </button>
                <button
                    style={{ ...styles.btn, background: "#dc3545" }}
                    disabled={busy || !running}
                    onClick={onStop}
                >
                    ⏹ 자동매매 중지
                </button>
                <button style={styles.btn} disabled={busy} onClick={onAccounts}>
                    💳 계정(잔액) 조회
                </button>
            </div>
            <p style={styles.helperText}>
                마지막 액션: <strong>{lastAction}</strong>
            </p>
        </section>
    );
}
