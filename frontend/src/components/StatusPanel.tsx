import * as styles from "../styles";
import type { ConnectionState } from "../api/types";

interface Props {
    statusText: string;
    running: boolean;
    connectionState: ConnectionState;
}

const CONNECTION_LABEL: Record<ConnectionState, string> = {
    connecting: "● 연결 중...",
    open: "● 실시간 연결됨",
    error: "● 연결 끊김",
};

const CONNECTION_COLOR: Record<ConnectionState, string> = {
    connecting: "#ffc107",
    open: "#28a745",
    error: "#dc3545",
};

export default function StatusPanel({ statusText, running, connectionState }: Props) {
    return (
        <>
            <header style={styles.header}>
                <h1 style={{ margin: 0 }}>Upbit AutoTrader (Frontend)</h1>
                <div style={styles.headerRight}>
                    <span style={{ ...styles.connectionIndicator, color: CONNECTION_COLOR[connectionState] }}>
                        {CONNECTION_LABEL[connectionState]}
                    </span>
                    <div style={{ ...styles.badge, background: running ? "#28a745" : "#6c757d" }}>
                        {running ? "RUNNING" : "IDLE"}
                    </div>
                </div>
            </header>

            <section style={styles.section}>
                <h2>상태</h2>
                <div style={styles.card}>
                    <p style={{ whiteSpace: "pre-wrap", margin: 0 }}>{statusText}</p>
                </div>
            </section>
        </>
    );
}
