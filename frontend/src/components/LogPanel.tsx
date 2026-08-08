import * as styles from "../styles";
import type { LogItem } from "../api/types";

interface Props {
    logs: LogItem[];
}

export default function LogPanel({ logs }: Props) {
    return (
        <section style={styles.section}>
            <h2>활동 로그</h2>
            <div style={styles.scrollCard(220)}>
                {logs.length === 0 ? (
                    <p style={styles.emptyText}>로그가 없습니다.</p>
                ) : (
                    <ul style={{ paddingLeft: 16, margin: 0 }}>
                        {logs.map((l, i) => (
                            <li key={i} style={{ marginBottom: 6 }}>
                                <code style={{ color: "#6c757d" }}>[{l.ts}]</code> {l.message}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </section>
    );
}
