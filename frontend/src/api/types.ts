export interface Account {
    currency: string;
    balance: string;
    locked: string;
    avg_buy_price: string;
    unit_currency: string;
}

export type NotificationType = "BUY" | "SELL" | "INFO" | "WARNING" | "ERROR";

export interface TradeNotification {
    message: string;
    type: NotificationType;
    timestamp: string;
    market: string;
}

export interface TradingSettings {
    markets: string[];
    rsiOversold: number;
    rsiOverbought: number;
    rsiPeriod: number;
    candleMinutes: number;
    candleCount: number;
    rsiCheckCooldownMs: number;
    minOrderKrw: number;
}

export type ConnectionState = "connecting" | "open" | "error";

export interface LogItem {
    ts: string;
    message: string;
}
