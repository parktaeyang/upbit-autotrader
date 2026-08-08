import * as styles from "../styles";
import type { TradeNotification } from "../api/types";

interface Props {
    notifications: TradeNotification[];
}

export default function NotificationPanel({ notifications }: Props) {
    return (
        <section style={styles.section}>
            <h2>매매 알림</h2>
            <div style={styles.scrollCard(300)}>
                {notifications.length === 0 ? (
                    <p style={styles.emptyText}>알림이 없습니다.</p>
                ) : (
                    <ul style={{ paddingLeft: 16, margin: 0 }}>
                        {notifications.map((notif, i) => {
                            const timestamp = notif.timestamp
                                ? new Date(notif.timestamp).toLocaleString("ko-KR")
                                : "";
                            return (
                                <li key={i} style={{ marginBottom: 8, listStyle: "none" }}>
                                    <div style={{ display: "flex", alignItems: "flex-start", gap: 8 }}>
                                        <span
                                            style={{
                                                color: styles.notificationTypeColor(notif.type),
                                                fontWeight: "bold",
                                                fontSize: "12px",
                                                minWidth: "60px",
                                            }}
                                        >
                                            [{notif.type}]
                                        </span>
                                        <div style={{ flex: 1 }}>
                                            {timestamp && (
                                                <code style={{ color: "#6c757d", fontSize: "11px" }}>
                                                    [{timestamp}]
                                                </code>
                                            )}
                                            <span style={{ marginLeft: 8 }}>{notif.message}</span>
                                        </div>
                                    </div>
                                </li>
                            );
                        })}
                    </ul>
                )}
            </div>
        </section>
    );
}
