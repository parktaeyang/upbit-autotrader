import type { CSSProperties } from "react";

export const page: CSSProperties = {
    maxWidth: 960,
    margin: "0 auto",
    padding: "24px",
    fontFamily: "Inter, system-ui, sans-serif",
};

export const header: CSSProperties = {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 16,
};

export const headerRight: CSSProperties = { display: "flex", alignItems: "center", gap: 10 };

export const badge: CSSProperties = {
    color: "#ffffff",
    padding: "6px 10px",
    borderRadius: 8,
    fontSize: 12,
    letterSpacing: 0.5,
};

export const connectionIndicator: CSSProperties = {
    fontSize: 12,
    display: "flex",
    alignItems: "center",
    gap: 6,
    color: "#888",
};

export const section: CSSProperties = { marginTop: 20 };

// 카드는 페이지 배경(다크/라이트 모두 가능)과 무관하게 항상 밝은 배경 위 어두운 텍스트로 고정한다.
// (이전에는 background/color가 모두 #000000으로 겹쳐 시스템 라이트 테마에서 텍스트가 보이지 않는 문제가 있었다.)
export const card: CSSProperties = {
    border: "1px solid #e5e7eb",
    borderRadius: 8,
    padding: 16,
    background: "#f9fafb",
    color: "#111827",
    boxShadow: "0 1px 2px rgba(0,0,0,0.04)",
};

export const scrollCard = (maxHeight: number): CSSProperties => ({
    ...card,
    maxHeight,
    overflow: "auto",
});

export const emptyText: CSSProperties = { margin: 0, color: "#888" };

export const buttons: CSSProperties = { display: "flex", gap: 10, flexWrap: "wrap" };

// 버튼 배경(회색/파랑/빨강)이 무엇이든 흰색 텍스트로 고정해 대비를 보장한다.
export const btn: CSSProperties = {
    appearance: "none",
    border: "none",
    background: "#6c757d",
    color: "#ffffff",
    padding: "10px 14px",
    borderRadius: 8,
    cursor: "pointer",
};

export const pre: CSSProperties = { margin: 0, fontSize: 12, lineHeight: 1.4 };

export const formGrid: CSSProperties = {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
    gap: 12,
};

export const formRow: CSSProperties = { display: "flex", flexDirection: "column", gap: 4 };

export const formLabel: CSSProperties = { fontSize: 13, color: "#555" };

export const formInput: CSSProperties = {
    padding: "8px 10px",
    borderRadius: 6,
    border: "1px solid #ccc",
    fontSize: 14,
};

export const helperText: CSSProperties = { color: "#666", marginTop: 8 };

export const divider: CSSProperties = { border: "none", borderTop: "1px solid #e5e7eb", margin: "16px 0" };

export const table: CSSProperties = { width: "100%", borderCollapse: "collapse", marginTop: 8 };

export const th: CSSProperties = {
    textAlign: "left",
    padding: "6px 8px",
    borderBottom: "1px solid #e5e7eb",
    fontSize: 13,
    color: "#555",
};

export const td: CSSProperties = {
    padding: "6px 8px",
    borderBottom: "1px solid #eee",
    fontSize: 14,
};

export function notificationTypeColor(type: string): string {
    switch (type) {
        case "BUY":
            return "#28a745";
        case "SELL":
        case "ERROR":
            return "#dc3545";
        case "WARNING":
            return "#ffc107";
        default:
            return "#6c757d";
    }
}
