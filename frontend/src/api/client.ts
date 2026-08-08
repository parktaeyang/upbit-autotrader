export const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8081";

async function request(path: string, options?: RequestInit): Promise<string> {
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
}

function parseJson(text: string): unknown {
    if (!text) return null;
    try {
        return JSON.parse(text);
    } catch {
        // 백엔드가 text를 주는 경우도 있어 안전하게 처리
        return text;
    }
}

export async function apiText(path: string, options?: RequestInit): Promise<string> {
    return request(path, options);
}

export async function apiGet<T>(path: string): Promise<T> {
    const text = await request(path);
    return parseJson(text) as T;
}

export async function apiPost(path: string, body?: unknown): Promise<string> {
    return request(path, {
        method: "POST",
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
    const text = await request(path, {
        method: "PUT",
        body: JSON.stringify(body),
    });
    return parseJson(text) as T;
}
