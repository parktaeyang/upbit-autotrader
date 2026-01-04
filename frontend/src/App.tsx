import { useEffect, useMemo, useRef, useState } from "react";

const API_BASE =
    import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

type LogItem = { ts: string; message: string };

interface TradeNotification {
    message: string;
    type: string; // "BUY", "SELL", "INFO", "WARNING", "ERROR"
    timestamp: string;
    market: string;
}

function App() {
    const [statusText, setStatusText] = useState<string>("Loading...");
    const [busy, setBusy] = useState<boolean>(false);
    interface Account {
        currency: string;
        balance: string;
        locked: string;
        avg_buy_price: string;
        unit_currency: string;
    }
    const [accounts, setAccounts] = useState<Account[] | null>(null);
    const [lastAction, setLastAction] = useState<string>("-");
    const [logs, setLogs] = useState<LogItem[]>([]);
    const [notifications, setNotifications] = useState<TradeNotification[]>([]);
    const pollRef = useRef<number | null>(null);
    const priceCheckRef = useRef<number | null>(null);
    const notificationPollRef = useRef<number | null>(null);

    const addLog = (message: string) => {
        const ts = new Date().toLocaleString();
        setLogs((prev) => [{ ts, message }, ...prev].slice(0, 200));
    };

    const apiText = useMemo(
        () =>
            async (path: string, options?: RequestInit) => {
                const res = await fetch(`${API_BASE}${path}`, {
                    method: "GET",
                    ...options,
                    headers: {
                        "Content-Type": "application/json",
                        ...(options?.headers || {}),
                    },
                });
                const text = await res.text();
                if (!res.ok) throw new Error(text || `HTTP ${res.status}`);
                return text;
            },
        []
    );

    const apiJson = useMemo(
        () =>
            async (path: string, options?: RequestInit) => {
                const res = await fetch(`${API_BASE}${path}`, {
                    method: "GET",
                    ...options,
                    headers: {
                        "Content-Type": "application/json",
                        ...(options?.headers || {}),
                    },
                });
                const text = await res.text();
                let data: unknown = null;
                try {
                    data = text ? (JSON.parse(text) as Account[]) : null;
                } catch {
                    // 백엔드가 text를 주는 경우도 있어 안전하게 처리
                    data = text;
                }
                if (!res.ok) {
                    throw new Error(
                        typeof data === "string" ? data : JSON.stringify(data)
                    );
                }
                return data;
            },
        []
    );

    const fetchStatus = async () => {
        try {
            const text = await apiText("/api/upbit/auto/status");
            setStatusText(text);
        } catch (e: unknown) {
            if (e instanceof Error) {
                setStatusText(`❌ 오류: ${e.message}`);
            } else {
                setStatusText(`❌ 알 수 없는 오류`);
            }
        }
    };

    const handleStart = async () => {
        setBusy(true);
        try {
            const text = await apiText("/api/upbit/auto/start", { method: "POST" });
            setLastAction("자동매매 시작");
            addLog(`▶️ START: ${text}`);
            await fetchStatus();
        } catch (e: unknown) {
            if (e instanceof Error) {
                setStatusText(`❌ 오류: ${e.message}`);
            } else {
                setStatusText(`❌ 알 수 없는 오류`);
            }
        } finally {
            setBusy(false);
        }
    };

    const handleStop = async () => {
        setBusy(true);
        try {
            const text = await apiText("/api/upbit/auto/stop", { method: "POST" });
            setLastAction("자동매매 중지");
            addLog(`⏹ STOP: ${text}`);
            await fetchStatus();
        } catch (e: unknown) {
            if (e instanceof Error) {
                setStatusText(`❌ 오류: ${e.message}`);
            } else {
                setStatusText(`❌ 알 수 없는 오류`);
            }
        } finally {
            setBusy(false);
        }
    };

    const handleOrders = async () => {
        setBusy(true);
        try {
            const text = await apiText("/api/upbit/orders", { method: "POST" });
            setLastAction("균등 분배 매수 실행");
            addLog(`🛒 ORDERS: ${text}`);
        } catch (e: unknown) {
            if (e instanceof Error) {
                setStatusText(`❌ 오류: ${e.message}`);
            } else {
                setStatusText(`❌ 알 수 없는 오류`);
            }
        } finally {
            setBusy(false);
        }
    };

    const handleAccounts = async () => {
        setBusy(true);
        try {
            const data = await apiJson("/api/upbit/accounts");
            setAccounts(data);
            addLog(`💳 ACCOUNTS 조회 성공`);
        } catch (e: unknown) {
            if (e instanceof Error) {
                setStatusText(`❌ 오류: ${e.message}`);
            } else {
                setStatusText(`❌ 알 수 없는 오류`);
            }
        } finally {
            setBusy(false);
        }
    };

    const fetchPrices = async () => {
        try {
            const data = await apiJson("/api/upbit/prices") as Record<string, number>;
            if (data && Object.keys(data).length > 0) {
                const priceList = Object.entries(data)
                    .map(([market, price]) => `${market}: ${price.toLocaleString('ko-KR')}원`)
                    .join(", ");
                addLog(`📊 가격 정보: ${priceList}`);
            }
        } catch (e: unknown) {
            // 가격 조회 실패는 조용히 무시 (자동매매가 실행 중이 아닐 수 있음)
        }
    };

    const fetchNotifications = async () => {
        try {
            const data = await apiJson("/api/upbit/notifications") as TradeNotification[];
            if (data && Array.isArray(data)) {
                setNotifications(data);
            }
        } catch (e: unknown) {
            // 알림 조회 실패는 조용히 무시
        }
    };

    const schedulePriceCheck = () => {
        const now = new Date();
        const minutes = now.getMinutes();
        const seconds = now.getSeconds();
        const milliseconds = now.getMilliseconds();
        
        // 30분 간격으로 다음 실행 시간 계산
        const minutesRemainder = minutes % 30;
        let minutesUntilNext = 30 - minutesRemainder;
        
        // 현재가 정확히 30분 경계(00분 또는 30분)이고 초가 0이면 다음 30분 후
        if (minutesRemainder === 0 && seconds === 0 && milliseconds < 100) {
            minutesUntilNext = 30;
        }
        
        // 다음 30분 경계까지 남은 시간 계산 (밀리초 단위)
        const msUntilNext = (minutesUntilNext * 60 - seconds) * 1000 - milliseconds;
        
        // 다음 30분 경계에 실행하고, 이후 매 30분마다 실행
        const timeoutId = window.setTimeout(() => {
            fetchPrices();
            // 매 30분마다 실행 (30분 = 1,800,000 밀리초)
            priceCheckRef.current = window.setInterval(fetchPrices, 30 * 60 * 1000);
        }, msUntilNext);
        
        return timeoutId;
    };

    useEffect(() => {
        // 최초 1회 상태 조회
        fetchStatus();

        // 5초마다 상태 폴링
        pollRef.current = window.setInterval(fetchStatus, 5000);
        
        // 매 분 정각(00초)에 가격 정보 조회
        const initialTimeoutId = schedulePriceCheck();
        
        // 최초 1회 알림 조회
        fetchNotifications();
        
        // 2초마다 알림 폴링 (매매 알림이 빈번할 수 있음)
        notificationPollRef.current = window.setInterval(fetchNotifications, 2000);
        
        return () => {
            if (pollRef.current) window.clearInterval(pollRef.current);
            if (priceCheckRef.current) window.clearInterval(priceCheckRef.current);
            if (notificationPollRef.current) window.clearInterval(notificationPollRef.current);
            window.clearTimeout(initialTimeoutId);
        };
    }, []);

    const running =
        typeof statusText === "string" &&
        /실행|running|on|켜짐/i.test(statusText ?? "");

    return (
        <div style={styles.page}>
            <header style={styles.header}>
                <h1 style={{ margin: 0 }}>Upbit AutoTrader (Frontend)</h1>
                <div style={{ ...styles.badge, background: running ? "#28a745" : "#6c757d" }}>
                    {running ? "RUNNING" : "IDLE"}
                </div>
            </header>

            <section style={styles.section}>
                <h2>상태</h2>
                <div style={styles.card}>
                    <p style={{ whiteSpace: "pre-wrap", margin: 0 }}>{statusText}</p>
                </div>
            </section>

            <section style={styles.section}>
                <h2>컨트롤</h2>
                <div style={styles.buttons}>
                    <button style={styles.btn} disabled={busy} onClick={handleOrders}>
                        🛒 균등 분배 매수 (1회)
                    </button>
                    <button
                        style={{ ...styles.btn, background: "#0d6efd" }}
                        disabled={busy || running}
                        onClick={handleStart}
                    >
                        ▶️ 자동매매 시작
                    </button>
                    <button
                        style={{ ...styles.btn, background: "#dc3545" }}
                        disabled={busy || !running}
                        onClick={handleStop}
                    >
                        ⏹ 자동매매 중지
                    </button>
                    <button style={styles.btn} disabled={busy} onClick={handleAccounts}>
                        💳 계정(잔액) 조회
                    </button>
                </div>
                <p style={{ color: "#666", marginTop: 8 }}>
                    마지막 액션: <strong>{lastAction}</strong>
                </p>
            </section>

            <section style={styles.section}>
                <h2>계정 정보</h2>
                <div style={styles.card}>
                    {accounts ? (
                        <pre style={styles.pre}>{JSON.stringify(accounts, null, 2)}</pre>
                    ) : (
                        <p style={{ margin: 0, color: "#888" }}>조회된 정보가 없습니다.</p>
                    )}
                </div>
            </section>

            <section style={styles.section}>
                <h2>매매 알림</h2>
                <div style={{ ...styles.card, maxHeight: 300, overflow: "auto" }}>
                    {notifications.length === 0 ? (
                        <p style={{ margin: 0, color: "#888" }}>알림이 없습니다.</p>
                    ) : (
                        <ul style={{ paddingLeft: 16, margin: 0 }}>
                            {notifications.map((notif, i) => {
                                const getTypeColor = (type: string) => {
                                    switch (type) {
                                        case "BUY": return "#28a745";
                                        case "SELL": return "#dc3545";
                                        case "WARNING": return "#ffc107";
                                        case "ERROR": return "#dc3545";
                                        default: return "#6c757d";
                                    }
                                };
                                const timestamp = notif.timestamp ? new Date(notif.timestamp).toLocaleString('ko-KR') : '';
                                return (
                                    <li key={i} style={{ marginBottom: 8, listStyle: "none" }}>
                                        <div style={{ display: "flex", alignItems: "flex-start", gap: 8 }}>
                                            <span style={{ 
                                                color: getTypeColor(notif.type), 
                                                fontWeight: "bold",
                                                fontSize: "12px",
                                                minWidth: "60px"
                                            }}>
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

            <section style={styles.section}>
                <h2>활동 로그</h2>
                <div style={{ ...styles.card, maxHeight: 220, overflow: "auto" }}>
                    {logs.length === 0 ? (
                        <p style={{ margin: 0, color: "#888" }}>로그가 없습니다.</p>
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

            <footer style={{ color: "#888", fontSize: 12, marginTop: 24 }}>
                API BASE: <code>{API_BASE}</code>
            </footer>
        </div>
    );
}

const styles: Record<string, React.CSSProperties> = {
    page: { maxWidth: 960, margin: "0 auto", padding: "24px", fontFamily: "Inter, system-ui, sans-serif" },
    header: { display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 },
    badge: { color: "#000000", padding: "6px 10px", borderRadius: 8, fontSize: 12, letterSpacing: 0.5 },
    section: { marginTop: 20 },
    card: {
        border: "1px solid #e5e7eb",
        borderRadius: 8,
        padding: 16,
        background: "#000000",
        boxShadow: "0 1px 2px rgba(0,0,0,0.04)",
    },
    buttons: { display: "flex", gap: 10, flexWrap: "wrap" },
    btn: {
        appearance: "none",
        border: "none",
        background: "#6c757d",
        color: "#000000",
        padding: "10px 14px",
        borderRadius: 8,
        cursor: "pointer",
    },
    pre: { margin: 0, fontSize: 12, lineHeight: 1.4 },
};

export default App;