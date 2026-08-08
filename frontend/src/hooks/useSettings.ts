import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPut } from "../api/client";
import type { TradingSettings } from "../api/types";

export function useSettings() {
    const [settings, setSettings] = useState<TradingSettings | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    const reload = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await apiGet<TradingSettings>("/api/upbit/settings");
            setSettings(data);
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : "알 수 없는 오류");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        reload();
    }, [reload]);

    const save = useCallback(async (next: TradingSettings): Promise<boolean> => {
        setError(null);
        try {
            const data = await apiPut<TradingSettings>("/api/upbit/settings", next);
            setSettings(data);
            return true;
        } catch (e: unknown) {
            setError(e instanceof Error ? e.message : "알 수 없는 오류");
            return false;
        }
    }, []);

    return { settings, loading, error, save, reload };
}
