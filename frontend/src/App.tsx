import { useState } from "react";
import { API_BASE, apiGet, apiPost } from "./api/client";
import type { Account, LogItem } from "./api/types";
import { useAutoTradingEvents } from "./hooks/useAutoTradingEvents";
import { useSettings } from "./hooks/useSettings";
import StatusPanel from "./components/StatusPanel";
import ControlPanel from "./components/ControlPanel";
import PricePanel from "./components/PricePanel";
import AccountPanel from "./components/AccountPanel";
import SettingsPanel from "./components/SettingsPanel";
import NotificationPanel from "./components/NotificationPanel";
import LogPanel from "./components/LogPanel";
import * as styles from "./styles";

function App() {
    const { statusText, prices, notifications, connectionState } = useAutoTradingEvents();
    const settingsState = useSettings();

    const [busy, setBusy] = useState(false);
    const [accounts, setAccounts] = useState<Account[] | null>(null);
    const [lastAction, setLastAction] = useState<string>("-");
    const [logs, setLogs] = useState<LogItem[]>([]);
    const [actionError, setActionError] = useState<string | null>(null);

    const addLog = (message: string) => {
        const ts = new Date().toLocaleString();
        setLogs((prev) => [{ ts, message }, ...prev].slice(0, 200));
    };

    const runAction = async (label: string, fn: () => Promise<void>) => {
        setBusy(true);
        setActionError(null);
        try {
            await fn();
        } catch (e: unknown) {
            setActionError(e instanceof Error ? e.message : "알 수 없는 오류");
            addLog(`❌ ${label} 실패: ${e instanceof Error ? e.message : "알 수 없는 오류"}`);
        } finally {
            setBusy(false);
        }
    };

    const handleStart = () =>
        runAction("자동매매 시작", async () => {
            const text = await apiPost("/api/upbit/auto/start");
            setLastAction("자동매매 시작");
            addLog(`▶️ START: ${text}`);
        });

    const handleStop = () =>
        runAction("자동매매 중지", async () => {
            const text = await apiPost("/api/upbit/auto/stop");
            setLastAction("자동매매 중지");
            addLog(`⏹ STOP: ${text}`);
        });

    const handleOrders = () =>
        runAction("균등 분배 매수", async () => {
            const text = await apiPost("/api/upbit/orders");
            setLastAction("균등 분배 매수 실행");
            addLog(`🛒 ORDERS: ${text}`);
        });

    const handleAccounts = () =>
        runAction("계정 조회", async () => {
            const data = await apiGet<Account[]>("/api/upbit/accounts");
            setAccounts(data);
            addLog("💳 ACCOUNTS 조회 성공");
        });

    const running = /실행|running|on|켜짐/i.test(statusText ?? "");

    return (
        <div style={styles.page}>
            <StatusPanel statusText={statusText} running={running} connectionState={connectionState} />

            <ControlPanel
                busy={busy}
                running={running}
                lastAction={lastAction}
                onOrders={handleOrders}
                onStart={handleStart}
                onStop={handleStop}
                onAccounts={handleAccounts}
            />
            {actionError && <p style={{ color: "#dc3545" }}>❌ {actionError}</p>}

            <PricePanel prices={prices} />

            <AccountPanel accounts={accounts} />

            <SettingsPanel
                settings={settingsState.settings}
                loading={settingsState.loading}
                error={settingsState.error}
                running={running}
                onSave={settingsState.save}
            />

            <NotificationPanel notifications={notifications} />

            <LogPanel logs={logs} />

            <footer style={{ color: "#888", fontSize: 12, marginTop: 24 }}>
                API BASE: <code>{API_BASE}</code>
            </footer>
        </div>
    );
}

export default App;
