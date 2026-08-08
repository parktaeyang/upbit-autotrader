import { useEffect, useRef, useState } from "react";
import { API_BASE, apiGet, apiText } from "../api/client";
import type { ConnectionState, TradeNotification } from "../api/types";

const MAX_NOTIFICATIONS = 200;

interface PriceEvent {
    market: string;
    price: number;
}

/**
 * 백엔드 SSE(`/api/upbit/events`)를 구독해 상태/가격/알림을 실시간으로 받는다.
 * 마운트 시 한 번 REST로 초기 상태를 채운 뒤, 이후 갱신은 SSE 이벤트로만 반영한다.
 */
export function useAutoTradingEvents() {
    const [statusText, setStatusText] = useState<string>("Loading...");
    const [prices, setPrices] = useState<Record<string, number>>({});
    const [notifications, setNotifications] = useState<TradeNotification[]>([]);
    const [connectionState, setConnectionState] = useState<ConnectionState>("connecting");
    const sourceRef = useRef<EventSource | null>(null);

    useEffect(() => {
        let cancelled = false;

        const hydrate = async () => {
            try {
                const text = await apiText("/api/upbit/auto/status");
                if (!cancelled) setStatusText(text);
            } catch (e: unknown) {
                if (!cancelled) {
                    setStatusText(e instanceof Error ? `❌ 오류: ${e.message}` : "❌ 알 수 없는 오류");
                }
            }
            try {
                const data = await apiGet<Record<string, number>>("/api/upbit/prices");
                if (!cancelled && data) setPrices(data);
            } catch {
                // 가격 조회 실패는 조용히 무시 (자동매매가 실행 중이 아닐 수 있음)
            }
            try {
                const data = await apiGet<TradeNotification[]>("/api/upbit/notifications");
                if (!cancelled && Array.isArray(data)) setNotifications(data);
            } catch {
                // 알림 조회 실패는 조용히 무시
            }
        };

        hydrate();

        setConnectionState("connecting");
        const source = new EventSource(`${API_BASE}/api/upbit/events`);
        sourceRef.current = source;

        source.onopen = () => setConnectionState("open");
        source.onerror = () => setConnectionState("error");

        source.addEventListener("status", (e: MessageEvent) => {
            setStatusText(e.data);
        });

        source.addEventListener("price", (e: MessageEvent) => {
            try {
                const { market, price } = JSON.parse(e.data) as PriceEvent;
                setPrices((prev) => ({ ...prev, [market]: price }));
            } catch {
                // 파싱 실패한 이벤트는 무시
            }
        });

        source.addEventListener("notification", (e: MessageEvent) => {
            try {
                const notification = JSON.parse(e.data) as TradeNotification;
                setNotifications((prev) => [notification, ...prev].slice(0, MAX_NOTIFICATIONS));
            } catch {
                // 파싱 실패한 이벤트는 무시
            }
        });

        return () => {
            cancelled = true;
            source.close();
            sourceRef.current = null;
        };
    }, []);

    return { statusText, prices, notifications, connectionState };
}
